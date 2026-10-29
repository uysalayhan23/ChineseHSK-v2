#!/usr/bin/env python3
"""Fetch and verify the Apache-2.0 Kokoro v1.1 Mandarin offline TTS pack."""
from __future__ import annotations

import hashlib
import json
import shutil
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
ASSETS = APP / "src/main/assets/mandarin_tts"
ASSET_ROOT = APP / "src/main/assets"
JNI = APP / "src/main/jniLibs"
CAST_FILE = ASSET_ROOT / "chinese_course/voice_cast.json"
MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_1.tar.bz2"
MODEL_SHA256 = "b78e1ac42b852e839cceaafbe88f66806bf5860992d1d7b9a3f2ad1198427aed"
RUNTIME_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-android.tar.bz2"
RUNTIME_SHA256 = "2ff63469a71cb6009aa2e3ed5f4a670f8abdcbe4bb9ffd23776afc792a6b4f44"
MODEL_ROOT = "kokoro-multi-lang-v1_1"
MODEL_FILES = {
    "model.onnx", "voices.bin", "tokens.txt", "lexicon-zh.txt",
    "lexicon-us-en.txt", "date-zh.fst", "number-zh.fst", "phone-zh.fst",
    "LICENSE",
}
FEMALE_VOICES = [
    "zf_001", "zf_002", "zf_003", "zf_004", "zf_005", "zf_006", "zf_007", "zf_008",
    "zf_017", "zf_018", "zf_019", "zf_021", "zf_022", "zf_023", "zf_024", "zf_026",
    "zf_027", "zf_028", "zf_032", "zf_036", "zf_038", "zf_039", "zf_040", "zf_042",
    "zf_043", "zf_044", "zf_046", "zf_047", "zf_048", "zf_049", "zf_051", "zf_059",
    "zf_060", "zf_067", "zf_070", "zf_071", "zf_072", "zf_073", "zf_074", "zf_075",
    "zf_076", "zf_077", "zf_078", "zf_079", "zf_083", "zf_084", "zf_085", "zf_086",
    "zf_087", "zf_088", "zf_090", "zf_092", "zf_093", "zf_094", "zf_099",
]
MALE_VOICES = [
    "zm_009", "zm_010", "zm_011", "zm_012", "zm_013", "zm_014", "zm_015", "zm_016",
    "zm_020", "zm_025", "zm_029", "zm_030", "zm_031", "zm_033", "zm_034", "zm_035",
    "zm_037", "zm_041", "zm_045", "zm_050", "zm_052", "zm_053", "zm_054", "zm_055",
    "zm_056", "zm_057", "zm_058", "zm_061", "zm_062", "zm_063", "zm_064", "zm_065",
    "zm_066", "zm_068", "zm_069", "zm_080", "zm_081", "zm_082", "zm_089", "zm_091",
    "zm_095", "zm_096", "zm_097", "zm_098", "zm_100",
]
LIBS = {"arm64-v8a", "armeabi-v7a"}
LIB_NAMES = {
    "libsherpa-onnx-c-api.so", "libonnxruntime.so",
    "libsherpa-onnx-jni.so", "libsherpa-onnx-cxx-api.so",
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def fetch(url: str, target: Path, expected: str) -> None:
    if target.exists() and sha256_file(target) == expected:
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "ChineseHSK-v2-build"})
    with urllib.request.urlopen(request, timeout=120) as response, target.open("wb") as out:
        shutil.copyfileobj(response, out)
    actual = sha256_file(target)
    if actual != expected:
        target.unlink(missing_ok=True)
        raise SystemExit(f"SHA-256 mismatch for {url}: {actual}")


def safe_members(archive: tarfile.TarFile):
    for member in archive.getmembers():
        path = Path(member.name)
        if path.is_absolute() or ".." in path.parts:
            raise SystemExit(f"Unsafe archive member: {member.name}")
        yield member


def write_voice_metadata() -> None:
    voices = []
    for gender, names, first_sid in (("female", FEMALE_VOICES, 3), ("male", MALE_VOICES, 58)):
        for index, name in enumerate(names):
            voices.append({
                "sid": first_sid + index,
                "datasetId": name,
                "ageGroup": "unspecified",
                "gender": gender,
                "accent": "Mandarin",
            })
    if len(voices) != 100 or len({row["datasetId"] for row in voices}) != 100:
        raise SystemExit("Kokoro Mandarin voice inventory is not 100 unique voices")
    (ASSET_ROOT / "mandarin_voice_inventory.json").write_text(
        json.dumps(voices, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    cast_data = json.loads(CAST_FILE.read_text(encoding="utf-8"))
    cast = cast_data.get("cast", [])
    if len(cast) != 83:
        raise SystemExit(f"Expected 83 canonical dialogue speakers, found {len(cast)}")
    by_gender = {
        "female": [row for row in voices if row["gender"] == "female"],
        "male": [row for row in voices if row["gender"] == "male"],
    }
    sorted_cast = sorted(cast, key=lambda row: row.get("voiceProfileId", ""))
    bindings = {}
    next_index = {"female": 0, "male": 0}
    for row in sorted_cast:
        profile = row.get("voiceProfileId")
        gender = row.get("genderPresentation")
        gender = gender if gender in by_gender else "female"
        candidates = by_gender[gender]
        index = next_index[gender] % len(candidates)
        voice = candidates[index]
        next_index[gender] += 1
        bindings[profile] = {
            "sid": voice["sid"],
            "voice": voice["datasetId"],
            "gender": gender,
            "ageClass": row.get("ageClass", "unspecified"),
        }
    # Reserve one distinct female voice for narrator and learning prompts.
    bindings["NARRATOR_ZH_001"] = {
        "sid": voices[0]["sid"], "voice": voices[0]["datasetId"],
        "gender": "female", "ageClass": "adult",
    }
    bindings["VOCAB_ZH_001"] = {
        "sid": voices[1]["sid"], "voice": voices[1]["datasetId"],
        "gender": "female", "ageClass": "adult",
    }
    (ASSET_ROOT / "kokoro_voice_bindings.json").write_text(
        json.dumps({"schemaVersion": 1, "model": "kokoro-multi-lang-v1_1", "bindings": bindings},
                   ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def main() -> None:
    temp = ROOT / ".build-cache/mandarin-tts"
    model_archive = temp / "kokoro-multi-lang-v1_1.tar.bz2"
    runtime_archive = temp / "sherpa-onnx-android.tar.bz2"
    fetch(MODEL_URL, model_archive, MODEL_SHA256)
    fetch(RUNTIME_URL, runtime_archive, RUNTIME_SHA256)

    if ASSETS.exists():
        shutil.rmtree(ASSETS)
    ASSETS.mkdir(parents=True, exist_ok=True)
    with tarfile.open(model_archive, "r:bz2") as archive:
        members = list(safe_members(archive))
        copied = set()
        for member in members:
            parts = Path(member.name).parts
            if len(parts) < 2 or parts[0] != MODEL_ROOT or not member.isfile():
                continue
            relative = Path(*parts[1:])
            if relative.name not in MODEL_FILES and parts[1] != "espeak-ng-data":
                continue
            target = ASSETS / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            source = archive.extractfile(member)
            if source is None:
                raise SystemExit(f"Could not read model member: {member.name}")
            with source, target.open("wb") as out:
                shutil.copyfileobj(source, out)
            copied.add(relative.as_posix())

    JNI.mkdir(parents=True, exist_ok=True)
    with tarfile.open(runtime_archive, "r:bz2") as archive:
        for member in safe_members(archive):
            parts = Path(member.name).parts
            if len(parts) < 3 or parts[-2] not in LIBS or Path(parts[-1]).name not in LIB_NAMES or not member.isfile():
                continue
            target = JNI / parts[-2] / Path(parts[-1]).name
            target.parent.mkdir(parents=True, exist_ok=True)
            source = archive.extractfile(member)
            if source is None:
                raise SystemExit(f"Could not read runtime member: {member.name}")
            with source, target.open("wb") as out:
                shutil.copyfileobj(source, out)

    required = MODEL_FILES | {
        "espeak-ng-data/phondata", "espeak-ng-data/phonindex", "espeak-ng-data/phontab",
        "espeak-ng-data/intonations", "espeak-ng-data/cmn_dict",
    }
    missing = sorted(path for path in required if not (ASSETS / path).is_file())
    for abi in LIBS:
        missing.extend(f"{abi}/{lib}" for lib in LIB_NAMES if not (JNI / abi / lib).is_file())
    if missing:
        raise SystemExit(f"Kokoro TTS pack is incomplete: {missing}")
    license_copy = ASSET_ROOT / "licenses/Kokoro-82M-v1.1-zh-LICENSE.txt"
    license_copy.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(ASSETS / "LICENSE", license_copy)
    write_voice_metadata()
    total = sum(path.stat().st_size for path in ASSETS.rglob("*") if path.is_file())
    print(f"Prepared Kokoro v1.1 Mandarin TTS ({total} model/runtime asset bytes), 100 Mandarin voices, and verified sherpa-onnx v1.13.8 Android libraries.")


if __name__ == "__main__":
    main()
