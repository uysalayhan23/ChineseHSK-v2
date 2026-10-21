#!/usr/bin/env python3
"""Check image-bound speaker anchors before they can be rendered by the app."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
COURSE = ROOT / "chinese_hsk_android_github" / "app" / "src" / "main" / "assets" / "chinese_course"
MANIFEST = ROOT / "ci" / "speaker_mouth_anchors.json"
RUNTIME_MANIFEST = COURSE / "speaker_mouth_anchors.json"
SCENE_ID = re.compile(r"^ZH_(HSK[1-6])_SC\d{3}$")


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def speakers_for(scene: dict) -> set[str]:
    return {
        str(row.get("speaker", "")).strip()
        for row in scene.get("dialogues", [])
        if str(row.get("speaker", "")).strip() not in {"", "旁白"}
    }


def anchors_for(entry: dict) -> set[str]:
    speakers = set(entry.get("anchors", {})) if isinstance(entry.get("anchors"), dict) else set()
    bindings = entry.get("dialogueAnchors", {})
    speakers.update(
        str(binding.get("speaker", "")).strip()
        for binding in bindings.values()
        if isinstance(binding, dict)
    )
    return speakers


def main() -> None:
    manifest = read_json(MANIFEST)
    runtime = read_json(RUNTIME_MANIFEST)
    if manifest != runtime:
        raise SystemExit("CI and runtime mouth-anchor manifests differ")

    entries = manifest.get("scenes", {})
    all_scene_paths = list((COURSE / "levels").glob("HSK[1-6]/scenes/ZH_HSK*_SC*.json"))
    scene_count = len(all_scene_paths)
    if scene_count != 300:
        raise SystemExit(f"Expected 300 runtime scenes; found {scene_count}")
    errors: list[str] = []
    passed = 0
    for scene_id, entry in entries.items():
        if entry.get("reviewStatus") != "VISUAL_QA_PASS":
            continue
        match = SCENE_ID.fullmatch(scene_id)
        if not match:
            errors.append(f"{scene_id}: invalid scene ID")
            continue
        level = match.group(1)
        scene_path = COURSE / "levels" / level / "scenes" / f"{scene_id}.json"
        image_path = COURSE / "media" / "scenes" / f"{scene_id}.webp"
        if not scene_path.is_file() or not image_path.is_file():
            errors.append(f"{scene_id}: missing scene JSON or source image")
            continue
        scene = read_json(scene_path)
        if scene.get("id") != scene_id:
            errors.append(f"{scene_id}: scene JSON identity mismatch")
            continue
        image_bytes = image_path.read_bytes()
        if hashlib.sha256(image_bytes).hexdigest() != entry.get("imageSha256"):
            errors.append(f"{scene_id}: image SHA-256 mismatch")
            continue
        with Image.open(image_path) as image:
            if list(image.size) != [entry.get("width"), entry.get("height")]:
                errors.append(f"{scene_id}: source image dimensions mismatch")
                continue

        anchors = anchors_for(entry)
        missing = speakers_for(scene) - anchors
        if missing:
            errors.append(f"{scene_id}: missing speaking-character anchors: {sorted(missing)}")
            continue
        points = list(entry.get("anchors", {}).values())
        points.extend(entry.get("personAnchors", {}).values())
        if any(
            not isinstance(point, list)
            or len(point) != 2
            or not all(isinstance(v, (int, float)) and 0 <= v <= 1 for v in point)
            for point in points
        ):
            errors.append(f"{scene_id}: invalid normalized anchor coordinate")
            continue
        scales = entry.get("bubbleScales", {})
        if any(not isinstance(v, (int, float)) or not 0.3 <= v <= 1.5 for v in scales.values()):
            errors.append(f"{scene_id}: bubble scale must be between 0.3 and 1.5")
            continue
        passed += 1

    if errors:
        raise SystemExit("\n".join(errors))
    print(f"PASS: {passed}/300 scenes have image-matched anchors for every speaking character; {300 - passed} still need visual calibration")


if __name__ == "__main__":
    main()
