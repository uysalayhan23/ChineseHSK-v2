#!/usr/bin/env python3
"""Fetch verified HSK3 SC047/SC029 WebP assets and prepare targeted repair.

Runs outside GitHub Actions. Does not generate images, publish files, or assign QA
PASS. The exact laptop-screen and donor quadrilaterals MUST be calibrated by
visual inspection of the downloaded originals before a repair is attempted.

  python repair_hsk3_sc047_fetch_and_run.py --prepare-only --workdir /tmp/sc047
  python repair_hsk3_sc047_fetch_and_run.py --workdir /tmp/sc047 \
    --target-quad 'TLx,TLy;TRx,TRy;BRx,BRy;BLx,BLy' \
    --donor-quad 'TLx,TLy;TRx,TRy;BRx,BRy;BLx,BLy'

The second form calls repair_hsk3_sc037_screen.py (pure image-region warp).
It checks SHA1 Git blob of both remote sources and then the underlying helper
checks all pixels outside the target screen stay EXACTLY unchanged in the
lossless WebP output. The output remains pending human LOCKED_V2 visual QA.
"""
from __future__ import annotations
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from urllib.request import Request, urlopen

OWNER_REPO = 'uysalayhan23/ChineseHSK-v2'
REPO_PREFIX = 'chinese_hsk_android_github/app/src/main/assets/chinese_course/media/scenes'
EXPECTED_BLOB = {
    '047': 'ba337c7c64bed0977bb5ab2942029eab08f88704',
    '029': 'f80a89a4c44243e056a27949e56ebab3ed7694a4',
}


def git_blob_sha(content: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(content)).encode('ascii') + b'\0' + content).hexdigest()


def decode_github_content(json_payload: bytes, expected_blob: str) -> bytes:
    obj = json.loads(json_payload)
    if obj.get('encoding') != 'base64' or not isinstance(obj.get('content'), str):
        raise ValueError('GitHub API did not return base64 file contents')
    raw = base64.b64decode(obj['content'], validate=False)
    actual = git_blob_sha(raw)
    if actual != expected_blob or obj.get('sha') != expected_blob:
        raise ValueError(f'Git blob SHA mismatch: expected {expected_blob}, got {actual}, API {obj.get("sha")}')
    if not raw.startswith(b'RIFF') or raw[8:12] != b'WEBP':
        raise ValueError('Verified blob is not a RIFF WebP file')
    return raw


def get_verified_webp(n: str) -> bytes:
    if n not in EXPECTED_BLOB:
        raise ValueError('Only locked HSK3 SC047 and SC029 are supported')
    path = f'{REPO_PREFIX}/ZH_HSK3_SC{n}.webp'
    url = f'https://api.github.com/repos/{OWNER_REPO}/contents/{path}?ref=main'
    headers = {'Accept': 'application/vnd.github+json', 'User-Agent': 'HSK3-locked-visual-audit'}
    if os.getenv('GITHUB_TOKEN'):
        headers['Authorization'] = 'Bearer ' + os.environ['GITHUB_TOKEN']
    with urlopen(Request(url, headers=headers), timeout=30) as response:
        return decode_github_content(response.read(), EXPECTED_BLOB[n])


def run(workdir: Path, *, target_quad: str | None = None,
        donor_quad: str | None = None, prepare_only: bool = False) -> Path | None:
    if not prepare_only and (not target_quad or not donor_quad):
        raise ValueError('Both accurately calibrated target and donor quads are required')
    # Fetch and VERIFY all bytes before writing either file.
    originals = {n: get_verified_webp(n) for n in ('047', '029')}
    workdir.mkdir(parents=True, exist_ok=True)
    paths = {}
    for n, raw in originals.items():
        path = workdir / f'ZH_HSK3_SC{n}_verified_original.webp'
        path.write_bytes(raw)
        paths[n] = path
        print(n, 'GIT_BLOB_SHA:', git_blob_sha(raw), 'BYTES:', len(raw))
    if prepare_only:
        print('PREPARED_ONLY: No WebP pixels modified; visually calibrate both quads.')
        return None
    helper = Path(__file__).resolve().with_name('repair_hsk3_sc037_screen.py')
    if not helper.is_file():
        raise FileNotFoundError(f'Required existing locked helper not found: {helper}')
    output = workdir / 'ZH_HSK3_SC047_repaired_QA_PENDING.webp'
    command = [sys.executable, str(helper), '--source', str(paths['047']),
               '--donor', str(paths['029']), '--output', str(output),
               '--target-quad', target_quad, '--donor-quad', donor_quad]
    subprocess.run(command, check=True)
    print('LOCKED_V2_QA_STATUS: PENDING_VISUAL_CANONICAL_REVIEW; do not publish')
    return output


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--workdir', type=Path, default=Path('sc047_repair_workspace'))
    p.add_argument('--prepare-only', action='store_true')
    p.add_argument('--target-quad', type=str)
    p.add_argument('--donor-quad', type=str)
    args = p.parse_args()
    run(args.workdir, target_quad=args.target_quad,
        donor_quad=args.donor_quad, prepare_only=args.prepare_only)


if __name__ == '__main__':
    main()
