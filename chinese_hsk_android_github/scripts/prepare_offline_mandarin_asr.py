#!/usr/bin/env python3
"""Fetch and safely install the pinned Vosk Mandarin offline model."""
from __future__ import annotations
import hashlib
import shutil
import tempfile
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath
ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/assets/vosk-model-small-cn-0.22"
URLS = ["https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip", "https://huggingface.co/rhasspy/vosk-models/resolve/main/zh/vosk-model-small-cn-0.22.zip?download=true"]
EXPECTED_SHA256 = "3af8b0e7e0f835ae9d414ce5df580237a3cfb08d586c9fbbb0f7ff29ad5b14ba"
REQUIRED = ("am/final.mdl", "conf/mfcc.conf")
def safe_extract(archive: zipfile.ZipFile, target: Path) -> None:
    root = target.resolve()
    for item in archive.infolist():
        name = item.filename.replace("\\", "/")
        rel = PurePosixPath(name)
        if rel.is_absolute() or ".." in rel.parts:
            raise RuntimeError(f"Unsafe archive member: {item.filename}")
        if item.is_dir(): continue
        parts = rel.parts[1:] if rel.parts and rel.parts[0] == "vosk-model-small-cn-0.22" else rel.parts
        if not parts: continue
        out = (target / Path(*parts)).resolve()
        if root not in out.parents: raise RuntimeError(f"Archive path escaped destination: {item.filename}")
        out.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(item) as src, out.open("wb") as dst: shutil.copyfileobj(src, dst)
def main() -> None:
    with tempfile.TemporaryDirectory(prefix="vosk-cn-") as temp:
        zip_path = Path(temp) / "model.zip"
        errors = []
        for url in URLS:
            try:
                req = urllib.request.Request(url, headers={"User-Agent": "ChineseHSK-v2-build"})
                with urllib.request.urlopen(req, timeout=180) as response, zip_path.open("wb") as out: shutil.copyfileobj(response, out)
                digest = hashlib.sha256(zip_path.read_bytes()).hexdigest()
                if digest != EXPECTED_SHA256: raise RuntimeError(f"SHA-256 mismatch: {digest}")
                break
            except Exception as exc:
                errors.append(f"{url}: {exc}")
                zip_path.unlink(missing_ok=True)
        else: raise RuntimeError("Could not fetch verified Vosk model. " + " | ".join(errors))
        staging = Path(temp) / "model"
        staging.mkdir()
        with zipfile.ZipFile(zip_path) as archive:
            bad = archive.testzip()
            if bad: raise RuntimeError(f"Corrupt ZIP member: {bad}")
            safe_extract(archive, staging)
        for rel in REQUIRED:
            file = staging / rel
            minimum = 1024 if rel == "am/final.mdl" else 1
            if not file.is_file() or file.stat().st_size < minimum: raise RuntimeError(f"Required model file missing or empty: {rel}")
        if DEST.exists(): shutil.rmtree(DEST)
        DEST.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(staging), str(DEST))
        print(f"Installed verified offline Mandarin model: {DEST}")
if __name__ == "__main__": main()
