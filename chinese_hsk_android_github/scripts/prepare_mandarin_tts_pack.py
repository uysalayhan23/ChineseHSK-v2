#!/usr/bin/env python3
"""Fetch pinned, freely licensed AISHELL-3 Mandarin TTS assets for APK builds."""
from __future__ import annotations

import hashlib
import shutil
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
ASSETS = APP / "src/main/assets/mandarin_tts"
JNI = APP / "src/main/jniLibs"
MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2"
MODEL_SHA256 = "ab468db3a3308cdd861495e0db2f25d79418a0c00639f74944c7cdf5dd8c6ec1"
RUNTIME_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-android.tar.bz2"
RUNTIME_SHA256 = "2ff63469a71cb6009aa2e3ed5f4a670f8abdcbe4bb9ffd23776afc792a6b4f44"
MODEL_FILES = {
    "model.onnx", "lexicon.txt", "tokens.txt", "phone.fst", "date.fst",
    "number.fst", "new_heteronym.fst", "speakers.txt",
}
# Phone builds only: omit desktop-emulator runtimes to keep the APK smaller.
LIBS = {"arm64-v8a", "armeabi-v7a"}
LIB_NAMES = {
    "libsherpa-onnx-c-api.so", "libonnxruntime.so",
    "libsherpa-onnx-jni.so", "libsherpa-onnx-cxx-api.so",
}


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


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def safe_members(archive: tarfile.TarFile):
    for member in archive.getmembers():
        path = Path(member.name)
        if path.is_absolute() or ".." in path.parts:
            raise SystemExit(f"Unsafe archive member: {member.name}")
        yield member


def main() -> None:
    temp = ROOT / ".build-cache/mandarin-tts"
    model_archive = temp / "aishell3.tar.bz2"
    runtime_archive = temp / "sherpa-onnx-android.tar.bz2"
    fetch(MODEL_URL, model_archive, MODEL_SHA256)
    fetch(RUNTIME_URL, runtime_archive, RUNTIME_SHA256)

    ASSETS.mkdir(parents=True, exist_ok=True)
    with tarfile.open(model_archive, "r:bz2") as archive:
        for member in safe_members(archive):
            name = Path(member.name).name
            if name not in MODEL_FILES or not member.isfile():
                continue
            source = archive.extractfile(member)
            if source is None:
                raise SystemExit(f"Could not read model member: {member.name}")
            with source, (ASSETS / name).open("wb") as out:
                shutil.copyfileobj(source, out)

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

    required = {name for name in MODEL_FILES if name != "speakers.txt"}
    missing = sorted(name for name in required if not (ASSETS / name).is_file())
    for abi in LIBS:
        missing.extend(f"{abi}/{lib}" for lib in LIB_NAMES if not (JNI / abi / lib).is_file())
    if missing:
        raise SystemExit(f"TTS pack is incomplete: {missing}")
    print(f"Prepared AISHELL-3 offline Mandarin TTS ({sum(p.stat().st_size for p in ASSETS.iterdir())} model bytes) and sherpa-onnx v1.13.8 Android libraries.")


if __name__ == "__main__":
    main()

