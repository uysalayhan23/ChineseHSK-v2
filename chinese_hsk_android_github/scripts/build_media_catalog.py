#!/usr/bin/env python3
"""Build reusable character/location/voice/media catalogs from the compiled course.
No binary media is required: the Android UI has deterministic Compose fallbacks.
When authored WebP/Opus assets are later dropped into the declared paths, the app
uses them without changing scene code.
"""
from __future__ import annotations
import hashlib, json, pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app" / "src" / "main" / "assets" / "chinese_course"


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def dump(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def theme_for(location_id: str):
    s = location_id.upper()
    table = [
        (("STATION", "TRAIN", "BUS", "TAXI", "AIRPORT"), ("transport", "🚉")),
        (("CAFE", "RESTAURANT", "CANTEEN", "KITCHEN"), ("cafe", "☕")),
        (("SCHOOL", "UNIVERSITY", "LIBRARY", "CLASS"), ("school", "学")),
        (("HOSPITAL", "CLINIC", "VET", "PHARMACY"), ("health", "医")),
        (("HOME", "HOUSE", "APARTMENT"), ("home", "家")),
        (("WORK", "OFFICE", "COMPANY", "MEETING"), ("work", "工")),
        (("PARK", "GARDEN", "WALKWAY", "RIVER"), ("nature", "🌳")),
        (("FARM", "FIELD", "MARKET"), ("market", "市")),
        (("HOTEL",), ("hotel", "旅")),
        (("BANK",), ("service", "银")),
        (("POST",), ("service", "邮")),
        (("SHOP", "SUPERMARKET", "STORE"), ("market", "店")),
        (("COMMUNITY", "HALL", "CENTER"), ("community", "人")),
    ]
    for keys, result in table:
        if any(k in s for k in keys): return result
    return "generic", "场"


def auto_character_id(name: str):
    digest = hashlib.sha1(name.encode("utf-8")).hexdigest()[:10].upper()
    return f"CHR_ZH_AUTO_{digest}"


def main():
    character_file = CONTENT / "characters.json"
    chars_root = load(character_file) if character_file.exists() else {"schemaVersion": 2, "characters": []}
    known = {c.get("nameZh"): c for c in chars_root.get("characters", []) if c.get("nameZh")}

    speakers = set()
    for level in range(1, 7):
        for scene_path in sorted((CONTENT / "levels" / f"HSK{level}" / "scenes").glob("*.json")):
            scene = load(scene_path)
            speakers.update(x for x in (scene.get("production") or {}).get("characters", []) if x)
            for d in scene.get("dialogues", []):
                if d.get("speaker"): speakers.add(d["speaker"])

    for name in sorted(speakers):
        if name in known: continue
        known[name] = {
            "id": auto_character_id(name),
            "nameZh": name,
            "pinyin": "",
            "role": "supporting_or_role",
        }

    # Enrich all characters with stable future asset/voice paths.
    characters = []
    for c in sorted(known.values(), key=lambda x: (x.get("role", ""), x.get("nameZh", ""))):
        c = dict(c)
        c.setdefault("role", "supporting")
        c["portraitAsset"] = c.get("portraitAsset") or f"chinese_course/media/characters/{c['id']}/base.webp"
        c["voiceProfileId"] = c.get("voiceProfileId") or f"VOICE_{c['id'].replace('CHR_', '')}"
        characters.append(c)
    dump(character_file, {"schemaVersion": 2, "characters": characters})

    # Location visual metadata.
    locations_path = CONTENT / "locations.json"
    loc_root = load(locations_path)
    locations = []
    for loc in loc_root.get("locations", []):
        loc = dict(loc)
        theme, icon = theme_for(loc["id"])
        loc["theme"] = theme
        loc["icon"] = icon
        loc["backgroundAsset"] = loc.get("backgroundAsset") or f"chinese_course/media/backgrounds/{loc['id']}_DAY.webp"
        locations.append(loc)
    dump(locations_path, {"schemaVersion": 2, "locations": locations})

    voices = [{
        "id": c["voiceProfileId"],
        "characterId": c["id"],
        "language": "zh-CN",
        "style": "android_native_mandarin_voice",
        "authoredAudioPreferred": False,
        "fallback": "android_tts"
    } for c in characters]
    voices.append({
        "id": "NARRATOR_ZH_001",
        "characterId": "NARRATOR",
        "language": "zh-CN",
        "style": "android_native_mandarin_voice",
        "authoredAudioPreferred": False,
        "fallback": "android_tts"
    })
    dump(CONTENT / "voices.json", {
        "schemaVersion": 1,
        "voices": voices,
        "runtimePolicy": {
            "canonicalEngine": "android_system_mandarin_tts",
            "characterLock": "character voiceProfileId -> best available zh-CN system voice -> persistent per-device binding",
            "synthesisMode": "direct_runtime_tts_no_generated_audio_files",
            "sceneDialogueProfileSource": "VoiceIdentityResolver.profileForSpeaker",
            "voiceGenderAndAge": "use explicit installed voice labels when available; Android TTS does not guarantee these labels",
            "crossDeviceExactTimbreGuaranteed": False
        }
    })

    media = CONTENT / "media"
    dump(media / "audio_manifest.json", {
        "schemaVersion": 1,
        "policy": "android_native_mandarin_tts_from_dialogue_text",
        "dialogueConvention": "chinese_course/media/audio/dialogues/<DIALOGUE_ID>.opus",
        "narratorConvention": "chinese_course/media/audio/narrator/<SCENE_ID>.opus",
        "ambienceConvention": "chinese_course/media/ambience/<LOCATION_ID>.opus",
        "musicConvention": "chinese_course/media/music/<SCENE_ID>.opus",
        "assets": {}
    })
    dump(media / "visual_manifest.json", {
        "schemaVersion": 1,
        "policy": "authored_webp_then_compose_fallback",
        "backgrounds": [{"locationId": x["id"], "asset": x["backgroundAsset"], "status": "planned"} for x in locations],
        "characters": [{"characterId": x["id"], "asset": x["portraitAsset"], "status": "planned"} for x in characters]
    })

    status_path = CONTENT / "content_status.json"
    if status_path.exists():
        status = load(status_path)
        status["mediaCatalog"] = {
            "characterRoleProfiles": len(characters),
            "locations": len(locations),
            "voiceProfiles": len(voices),
            "visualFallbackReady": True,
            "audioFallbackReady": True,
            "authoredBinaryMediaStatus": "not_yet_bundled"
        }
        dump(status_path, status)
    print(f"Media catalog: {len(characters)} character/role profiles, {len(locations)} locations, {len(voices)} voices.")


if __name__ == "__main__":
    main()
