#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "chinese_hsk_android_github" / "app" / "src" / "main"
ASSETS = APP / "assets" / "chinese_course"
JAVA = APP / "java" / "com" / "ayhan" / "chineselearning"

errors: list[str] = []
warnings: list[str] = []
notes: list[str] = []

def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))

chars_doc = load_json(ASSETS / "characters.json")
voices_doc = load_json(ASSETS / "voices.json")
cast_doc = load_json(ROOT / "ci" / "voice_cast_manifest.json")

chars = chars_doc.get("characters", [])
voices = voices_doc.get("voices", [])
cast = cast_doc.get("cast", [])

char_by_name = {x["nameZh"]: x for x in chars}
voice_by_id = {x["id"]: x for x in voices}
cast_by_name = {x["speaker"]: x for x in cast}

if len(chars) != len(char_by_name):
    errors.append("characters.json contains duplicate nameZh values")
if len(voices) != len(voice_by_id):
    errors.append("voices.json contains duplicate voice IDs")
if len(cast) != len(cast_by_name):
    errors.append("voice_cast_manifest.json contains duplicate speaker rows")

profile_to_chars = defaultdict(list)
for ch in chars:
    p = ch.get("voiceProfileId", "")
    if not p:
        errors.append(f"Character missing voiceProfileId: {ch.get('id')} / {ch.get('nameZh')}")
        continue
    profile_to_chars[p].append(ch.get("nameZh"))
    if p not in voice_by_id:
        errors.append(f"Character profile not present in voices.json: {ch.get('nameZh')} -> {p}")

for profile, names in sorted(profile_to_chars.items()):
    if len(names) > 1:
        errors.append(f"voiceProfileId reused by multiple characters: {profile} -> {names}")

for speaker, row in cast_by_name.items():
    ch = char_by_name.get(speaker)
    if ch is None:
        errors.append(f"Cast speaker missing from characters.json: {speaker}")
        continue
    if row.get("voiceProfileId") != ch.get("voiceProfileId"):
        errors.append(
            f"Cast/profile mismatch for {speaker}: cast={row.get('voiceProfileId')} "
            f"characters={ch.get('voiceProfileId')}"
        )

for name in char_by_name:
    if name not in cast_by_name:
        errors.append(f"Character missing from voice cast: {name}")

scene_count = 0
dialogue_count = 0
speaker_scene_usage: dict[str, set[str]] = defaultdict(set)
speaker_line_usage = Counter()
speed_by_level: dict[str, list[float]] = defaultdict(list)
speed_by_scene: dict[str, float] = {}
narrator_profiles = Counter()
unknown_speakers: dict[str, list[str]] = defaultdict(list)
bad_dialogue_lines: list[str] = []
scene_count_by_level = Counter()
dialogue_count_by_level = Counter()

for level_num in range(1, 7):
    level = f"HSK{level_num}"
    scene_dir = ASSETS / "levels" / level / "scenes"
    files = sorted(scene_dir.glob(f"ZH_{level}_SC*.json"))
    scene_count_by_level[level] = len(files)
    if len(files) != 50:
        errors.append(f"{level}: expected 50 scenes, found {len(files)}")

    for scene_file in files:
        scene = load_json(scene_file)
        scene_id = scene.get("id", scene_file.stem)
        scene_count += 1
        dialogues = scene.get("dialogues", [])
        dialogue_count += len(dialogues)
        dialogue_count_by_level[level] += len(dialogues)
        if len(dialogues) != 100:
            errors.append(f"{scene_id}: expected 100 dialogues, found {len(dialogues)}")

        speed = scene.get("learning", {}).get("defaultSpeechSpeed")
        if not isinstance(speed, (int, float)):
            errors.append(f"{scene_id}: missing/non-numeric defaultSpeechSpeed")
        else:
            speed = float(speed)
            speed_by_level[level].append(speed)
            speed_by_scene[scene_id] = speed
            if not 0.65 <= speed <= 1.35:
                errors.append(f"{scene_id}: defaultSpeechSpeed outside runtime bounds: {speed}")

        narrator_profile = (
            scene.get("narrator", {}).get("profileId")
            or scene.get("production", {}).get("audio", {}).get("narratorProfile")
            or "NARRATOR_ZH_001"
        )
        narrator_profiles[narrator_profile] += 1
        if narrator_profile not in voice_by_id:
            errors.append(f"{scene_id}: narrator profile missing from voices.json: {narrator_profile}")

        for idx, line in enumerate(dialogues, start=1):
            did = line.get("id", "")
            speaker = line.get("speaker", "")
            zh = line.get("zh", "")
            if not did or not speaker or not zh:
                bad_dialogue_lines.append(f"{scene_id} line {idx}: id/speaker/zh missing")
                continue
            speaker_line_usage[speaker] += 1
            speaker_scene_usage[speaker].add(scene_id)
            if speaker not in char_by_name:
                unknown_speakers[speaker].append(scene_id)

if bad_dialogue_lines:
    errors.extend(bad_dialogue_lines[:50])
    if len(bad_dialogue_lines) > 50:
        errors.append(f"... plus {len(bad_dialogue_lines)-50} more malformed dialogue lines")

for speaker, scenes in sorted(unknown_speakers.items()):
    errors.append(f"Unknown dialogue speaker {speaker}: {sorted(set(scenes))[:20]}")

if scene_count != 300:
    errors.append(f"Expected 300 total scenes, found {scene_count}")
if dialogue_count != 30000:
    errors.append(f"Expected 30000 total dialogues, found {dialogue_count}")

# Every speaker used in scene content must resolve to one stable profile.
resolved_profiles = {}
for speaker in sorted(speaker_line_usage):
    ch = char_by_name.get(speaker)
    if ch:
        resolved_profiles[speaker] = ch["voiceProfileId"]

# Runtime implementation checks.
kt_files = sorted(JAVA.glob("*.kt"))
direct_tts_imports = []
direct_tts_constructors = []
mandarin_users = []
dialogue_player_users = []

for f in kt_files:
    text = f.read_text(encoding="utf-8")
    if "android.speech.tts.TextToSpeech" in text:
        direct_tts_imports.append(f.name)
    if re.search(r"\bTextToSpeech\s*\(", text):
        direct_tts_constructors.append(f.name)
    if "MandarinTtsPlayer(" in text:
        mandarin_users.append(f.name)
    if "DialogueAudioPlayer(" in text:
        dialogue_player_users.append(f.name)

if direct_tts_imports != ["MandarinTtsPlayer.kt"]:
    errors.append(f"TextToSpeech import exists outside canonical wrapper: {direct_tts_imports}")
if direct_tts_constructors != ["MandarinTtsPlayer.kt"]:
    errors.append(f"TextToSpeech constructor exists outside canonical wrapper: {direct_tts_constructors}")

required_runtime_files = {
    "DialogueAudioPlayer.kt": [
        "private val tts = MandarinTtsPlayer(context)",
        "private val voiceResolver = VoiceIdentityResolver(context)",
        "voiceResolver.profileForSpeaker(dialogue.speaker)",
        "val path = generatedDialoguePath(dialogue.id)",
        'it.startsWith("chinese_course/media/audio/generated/")',
        'val stableCacheId = "VOICE_" + profileId + "_" + dialogue.id + "_" + textRevision',
        "tts.playOrCache(stableCacheId, dialogue.zh, profileId, speed, onDone)",
        'tts.playOrCache("NARRATOR_$sceneId", narrator.zh, narrator.profileId, speed, onDone)',
    ],
    "VoiceIdentityResolver.kt": [
        '.open("chinese_course/voices.json")',
        '.open("chinese_course/characters.json")',
        'item.optString("characterId", "").trim()',
        'item.optString("voiceProfileId", "").trim()',
        "bySpeaker[nameZh] = profile",
        "maps.bySpeaker[key]",
    ],
    "MandarinTtsPlayer.kt": [
        'getSharedPreferences("offline_mandarin_voice_bindings"',
        "localVoices[abs(profileId.hashCode()) % localVoices.size]",
        'prefs.edit().putString(prefKey, chosen.name).apply()',
        "tts.setPitch(deterministicPitch(profileId))",
        "playOrCache(stableTextKey, text, voiceProfileId, speed, onDone)",
    ],
    "MainActivity.kt": [
        "DialogueAudioPlayer(context)",
        "audioPlayer.playDialogue(current, speed)",
        "audioPlayer.playNarrator(scene.id, narrator, 0.90f)",
        "progress.playbackSpeed(scene.learning.defaultSpeechSpeed)",
    ],
}

for filename, needles in required_runtime_files.items():
    p = JAVA / filename
    if not p.exists():
        errors.append(f"Missing runtime source: {filename}")
        continue
    text = p.read_text(encoding="utf-8")
    for needle in needles:
        if needle not in text:
            errors.append(f"{filename}: required canonical voice path fragment missing: {needle}")

# Learning/utility screens should instantiate only the same wrapper.
expected_wrapper_users = {
    "AppFlowScreens.kt",
    "HabitScreens.kt",
    "LearningScreens.kt",
    "DialogueAudioPlayer.kt",
}
missing_wrapper_users = expected_wrapper_users - set(mandarin_users)
if missing_wrapper_users:
    errors.append(f"Expected MandarinTtsPlayer users missing: {sorted(missing_wrapper_users)}")

# Check known learning profile calls.
learning_checks = {
    "AppFlowScreens.kt": ['tts.speak(q.audioZh, "PLACEMENT", 0.88f)'],
    "HabitScreens.kt": ['tts.speak(card.zh, "WEAK_VOCAB", 0.82f)'],
    "LearningScreens.kt": [
        'tts.speak(card.zh, "VOCAB", 0.82f)',
        'tts.speak(item.zh, "PRONUNCIATION", 0.78f)',
        'tts.speak(item.promptZh, "DIALOGUE_PROMPT", 0.88f)',
    ],
}
for filename, needles in learning_checks.items():
    text = (JAVA / filename).read_text(encoding="utf-8")
    for needle in needles:
        if needle not in text:
            errors.append(f"{filename}: expected canonical learning speech call missing: {needle}")

# Speed behavior / important runtime notes.
progress_text = (JAVA / "ProgressStore.kt").read_text(encoding="utf-8")
main_text = (JAVA / "MainActivity.kt").read_text(encoding="utf-8")
if 'prefs.getFloat("playback_speed", defaultValue)' in progress_text:
    warnings.append(
        "A saved global playback_speed overrides per-scene defaultSpeechSpeed after the user changes speed."
    )
if 'listOf(0.75f, 0.85f, 1.0f, 1.15f, 1.25f)' in main_text:
    warnings.append(
        "Scene speed UI offers fixed presets 0.75/0.85/1.0/1.15/1.25; SC001 default 0.80 is used only before a global user override is saved."
    )
if 'audioPlayer.playNarrator(scene.id, narrator, 0.90f)' in main_text:
    notes.append("Narrator playback is intentionally fixed at 0.90x in MainActivity.")
if "levelOverrides" in json.dumps(cast_doc, ensure_ascii=False):
    warnings.append(
        "voice_cast_manifest levelOverrides (age/speed/pitch) are NOT consumed by the Android runtime TTS path; runtime character timbre/pitch stays profile-locked, while scene/global playback speed controls tempo."
    )

speed_summary = {}
for level, vals in sorted(speed_by_level.items()):
    speed_summary[level] = {
        "sceneCount": len(vals),
        "min": min(vals) if vals else None,
        "max": max(vals) if vals else None,
        "average": round(sum(vals) / len(vals), 4) if vals else None,
        "unique": sorted(set(vals)),
    }

report = {
    "status": "PASS" if not errors else "FAIL",
    "errors": errors,
    "warnings": warnings,
    "notes": notes,
    "counts": {
        "characters": len(chars),
        "voices": len(voices),
        "voiceCastRows": len(cast),
        "scenes": scene_count,
        "dialogues": dialogue_count,
        "usedDialogueSpeakers": len(speaker_line_usage),
    },
    "sceneCountByLevel": dict(sorted(scene_count_by_level.items())),
    "dialogueCountByLevel": dict(sorted(dialogue_count_by_level.items())),
    "speedSummary": speed_summary,
    "narratorProfiles": dict(narrator_profiles),
    "runtime": {
        "directTextToSpeechImports": direct_tts_imports,
        "directTextToSpeechConstructors": direct_tts_constructors,
        "mandarinTtsPlayerUsers": mandarin_users,
        "dialogueAudioPlayerUsers": dialogue_player_users,
        "resolvedSpeakerProfileCount": len(resolved_profiles),
        "genericFallbackNeededForDialogueSpeakers": bool(unknown_speakers),
    },
    "speakerUsage": [
        {
            "speaker": speaker,
            "profileId": resolved_profiles.get(speaker, ""),
            "sceneCount": len(speaker_scene_usage[speaker]),
            "dialogueCount": speaker_line_usage[speaker],
        }
        for speaker in sorted(speaker_line_usage)
    ],
}

out = ROOT / "voice_runtime_verification.json"
out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print(json.dumps(report, ensure_ascii=False, indent=2))
sys.exit(0 if not errors else 1)
