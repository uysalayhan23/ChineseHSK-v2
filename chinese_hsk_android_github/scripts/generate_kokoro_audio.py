#!/usr/bin/env python3
"""Generate resumable, per-turn Mandarin speech from the current course dialogue.

The committed voice cast is authoritative: each speaker keeps the named Kokoro
voice assigned in chinese_course/voice_cast.json. Output files are uncompressed
Opus assets so Android can open them through AssetManager.openFd.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
import shutil
import subprocess
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "chinese_course"
MEDIA = ASSETS / "media"
OUT = MEDIA / "audio" / "generated"
MANIFEST = MEDIA / "audio_manifest.json"
INVENTORY = MEDIA / "audio_binary_inventory.json"
REPO_ID = "hexgrad/Kokoro-82M"
SAMPLE_RATE = 24_000
OPUS_DECODED_SAMPLE_RATE = 48_000
BITRATE = "48k"


def load(path: pathlib.Path):
    return json.loads(path.read_text(encoding="utf-8"))


def dump(path: pathlib.Path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tmp.replace(path)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def opus_encode(ffmpeg: str, audio, dst: pathlib.Path, pitch_semitones: float):
    import numpy as np

    dst.parent.mkdir(parents=True, exist_ok=True)
    samples = np.asarray(audio, dtype=np.float32).reshape(-1)
    if samples.size < SAMPLE_RATE // 5 or not np.isfinite(samples).all():
        raise ValueError("Generated audio was empty, too short, or non-finite")
    cmd = [ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
           "-f", "f32le", "-ar", str(SAMPLE_RATE), "-ac", "1", "-i", "pipe:0"]
    if abs(pitch_semitones) >= 0.01:
        cmd += ["-af", f"rubberband=pitch={math.pow(2.0, pitch_semitones / 12.0):.8f}"]
    cmd += ["-c:a", "libopus", "-b:a", BITRATE, "-vbr", "on", "-compression_level", "10", str(dst)]
    result = subprocess.run(cmd, input=samples.tobytes(), stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    if result.returncode:
        dst.unlink(missing_ok=True)
        raise RuntimeError(result.stderr.decode("utf-8", "replace")[-2000:])


def collect(level: str | None):
    cast = {c.get("speaker", ""): c for c in load(ASSETS / "voice_cast.json").get("cast", [])}
    jobs = []
    for number in range(1, 7):
        level_name = f"HSK{number}"
        if level and level != level_name:
            continue
        scene_dir = ASSETS / "levels" / level_name / "scenes"
        for scene_path in sorted(scene_dir.glob("*.json")):
            scene = load(scene_path)
            for line in scene.get("dialogues", []):
                speaker = line.get("speaker", "").strip()
                voice = cast.get(speaker)
                if not voice:
                    raise ValueError(f"No locked voice cast for dialogue speaker {speaker!r} in {scene_path.name}")
                synthesis = voice.get("synthesis", {})
                voice_name = synthesis.get("voice", "")
                if not voice_name.startswith(("zf_", "zm_")):
                    raise ValueError(f"Unsupported Kokoro Mandarin voice {voice_name!r} for {speaker}")
                override = (voice.get("levelOverrides") or {}).get(level_name, {})
                jobs.append({
                    "id": line["id"], "text": line.get("zh", "").strip(),
                    "speaker": speaker, "voiceProfileId": voice.get("voiceProfileId", ""),
                    "voice": voice_name, "speed": float(override.get("speed", synthesis.get("speed", 1.0))),
                    "pitch": float(override.get("pitch", synthesis.get("pitchSemitones", 0.0))),
                    "level": level_name,
                })
    return jobs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--level", choices=[f"HSK{i}" for i in range(1, 7)])
    parser.add_argument("--limit", type=int, default=0, help="Generate at most this many missing files (0 = all)")
    parser.add_argument("--force", action="store_true", help="Regenerate existing files")
    parser.add_argument("--progress-every", type=int, default=100)
    parser.add_argument("--device", choices=["cuda", "cpu"], default=None)
    args = parser.parse_args()

    ffmpeg = shutil.which("ffmpeg")
    ffprobe = shutil.which("ffprobe")
    if not ffmpeg or not ffprobe:
        raise SystemExit("ffmpeg and ffprobe are required")

    jobs = collect(args.level)
    existing_manifest = load(MANIFEST) if MANIFEST.exists() else {"assets": {}}
    assets = dict(existing_manifest.get("assets", {}))
    inventory_root = load(INVENTORY) if INVENTORY.exists() else {"assets": []}
    inventory = {item["assetId"]: item for item in inventory_root.get("assets", [])}
    pending = [job for job in jobs if args.force or not (OUT / f"{job['id']}.opus").is_file()]
    if args.limit > 0:
        pending = pending[:args.limit]
    print(f"Current dialogues: {len(jobs)}; pending this run: {len(pending)}", flush=True)
    if not pending:
        return

    import numpy as np
    import torch
    from kokoro import KPipeline

    device = args.device or ("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Loading {REPO_ID} on {device}", flush=True)
    pipeline = KPipeline(lang_code="z", repo_id=REPO_ID, device=device)
    started = time.monotonic()
    failures = []
    for index, job in enumerate(pending, 1):
        dst = OUT / f"{job['id']}.opus"
        if not job["text"]:
            failures.append({"id": job["id"], "error": "empty Chinese text"})
            continue
        try:
            chunks = [audio.detach().cpu().numpy() for _, _, audio in pipeline(
                job["text"], voice=job["voice"], speed=max(0.75, min(1.25, job["speed"])), split_pattern=r"\n+"
            ) if audio is not None]
            if not chunks:
                raise ValueError("Kokoro returned no audio")
            opus_encode(ffmpeg, np.concatenate(chunks), dst, max(-1.75, min(1.75, job["pitch"])))
            rel = dst.relative_to(ROOT / "app" / "src" / "main" / "assets").as_posix()
            assets[job["id"]] = rel
            probe = json.loads(subprocess.check_output([
                ffprobe, "-v", "error", "-select_streams", "a:0",
                "-show_entries", "stream=codec_name,sample_rate,channels:format=duration",
                "-of", "json", str(dst)
            ], text=True))
            stream = (probe.get("streams") or [{}])[0]
            duration = float((probe.get("format") or {}).get("duration", 0))
            if stream.get("codec_name") != "opus" or int(stream.get("sample_rate", 0)) != OPUS_DECODED_SAMPLE_RATE or int(stream.get("channels", 0)) != 1 or duration <= 0:
                raise ValueError(f"unexpected encoded format: {probe}")
            inventory[job["id"]] = {
                "assetId": job["id"], "kind": "dialogue", "ownerId": job["id"].rsplit("_", 1)[0],
                "speaker": job["speaker"], "voiceProfileId": job["voiceProfileId"], "voice": job["voice"],
                "level": job["level"], "target": rel, "codec": "opus", "sampleRate": SAMPLE_RATE,
                "channels": 1, "durationSec": duration, "bytes": dst.stat().st_size, "sha256": sha256(dst),
            }
        except Exception as exc:
            failures.append({"id": job["id"], "error": str(exc)[:1000]})
            dst.unlink(missing_ok=True)
            assets.pop(job["id"], None)
            inventory.pop(job["id"], None)
        if index % max(1, args.progress_every) == 0 or index == len(pending):
            dump(MANIFEST, {"schemaVersion": 1, "policy": "kokoro_authored_dialogue_then_android_tts_fallback",
                            "model": REPO_ID, "assets": assets})
            dump(INVENTORY, {"schemaVersion": 1, "model": REPO_ID, "generatedCount": len(inventory),
                             "failureCountThisRun": len(failures), "failures": failures, "assets": list(inventory.values())})
            elapsed = max(0.001, time.monotonic() - started)
            print(f"{index}/{len(pending)} this run; total packaged {len(assets)}/{len(jobs)}; "
                  f"errors {len(failures)}; {index / elapsed:.2f} lines/sec", flush=True)

    if failures:
        failure_path = MEDIA / "audio_generation_failures.json"
        dump(failure_path, {"model": REPO_ID, "count": len(failures), "failures": failures})
        raise SystemExit(f"Audio generation failed for {len(failures)} lines; see {failure_path}")


if __name__ == "__main__":
    main()
