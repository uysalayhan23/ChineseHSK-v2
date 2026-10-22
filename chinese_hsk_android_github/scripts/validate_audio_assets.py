#!/usr/bin/env python3
from __future__ import annotations
import argparse, json, pathlib, shutil, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app' / 'src' / 'main' / 'assets'
MEDIA = ASSETS / 'chinese_course' / 'media'


def probe(path: pathlib.Path):
    cmd = ['ffprobe', '-v', 'error', '-select_streams', 'a:0',
           '-show_entries', 'stream=codec_name,sample_rate,channels', '-of', 'json', str(path)]
    data = json.loads(subprocess.check_output(cmd, text=True))
    return (data.get('streams') or [{}])[0]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--strict', action='store_true', help='require every queue entry to have a binary')
    args = ap.parse_args()
    if shutil.which('ffprobe') is None:
        raise SystemExit('ffprobe is required')
    q = json.loads((MEDIA / 'audio_asset_queue.json').read_text(encoding='utf-8'))
    errors = []; seen = set(); present = 0
    for e in q['entries']:
        aid = e['assetId']
        if aid in seen: errors.append('duplicate audio assetId ' + aid)
        seen.add(aid)
        dst = ASSETS / e['target']
        if not dst.exists():
            if args.strict or e.get('requiredForRelease'): errors.append('missing ' + aid)
            continue
        present += 1
        try:
            s = probe(dst)
            if s.get('codec_name') != 'opus': errors.append(f'{aid}: codec={s.get("codec_name")}, expected opus')
            # Opus is decoded at 48 kHz even when the encoder input was 24 kHz.
            expected_rate = 48_000 if s.get('codec_name') == 'opus' else int(e['sampleRate'])
            if int(s.get('sample_rate') or 0) != expected_rate: errors.append(f'{aid}: sample rate mismatch')
            if int(s.get('channels') or 0) != int(e['channels']): errors.append(f'{aid}: channel mismatch')
        except Exception as ex:
            errors.append(f'{aid}: unreadable audio: {ex}')
    print(f'Audio validation: present={present}/{len(q["entries"])}, errors={len(errors)}')
    for x in errors[:50]: print('ERROR:', x)
    if errors: sys.exit(1)

if __name__ == '__main__': main()
