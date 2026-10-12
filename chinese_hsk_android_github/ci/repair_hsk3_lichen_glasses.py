#!/usr/bin/env python3
"""Add thin rectangular glasses ONLY over Li Chen's eyes in SC038/039.

No scene regeneration. Manual landmark calibration and canonical visual review are
REQUIRED before acceptance; this script itself never assigns QA PASS.
"""
import argparse
import hashlib
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw


def git_blob_sha(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()


def point(value):
    try:
        x, y = map(float, value.split(','))
        return x, y
    except Exception as exc:
        raise argparse.ArgumentTypeError('Use x,y') from exc


def add_glasses(source, left, right):
    """Return corrected RGB image and mask (only alpha-covered eye pixels)."""
    if source.mode != 'RGB':
        source = source.convert('RGB')
    w, h = source.size
    if (w, h) != (941, 1672):
        raise ValueError('Expected canonical 941x1672 WebP')
    dx, dy = right[0] - left[0], right[1] - left[1]
    sep = math.hypot(dx, dy)
    if not (18 <= sep <= 135) or not all(0 <= p[0] < w and 0 <= p[1] < h for p in (left, right)):
        raise ValueError('Eye landmarks out of safe range')
    u = np.array((dx, dy), float) / sep
    v = np.array((-u[1], u[0]))
    scale = 4
    overlay = Image.new('RGBA', (w * scale, h * scale), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    def xy(center, ax, ay):
        p = np.array(center) + ax * u + ay * v
        return (round(p[0] * scale), round(p[1] * scale))
    # Thin dark rectangular glasses, soft bevels; width tuned by eye spacing.
    thickness = max(1.2, sep * .045)
    for eye in (left, right):
        width, height = sep * .84, sep * .56
        pts = [xy(eye, -width/2, -height/2), xy(eye, width/2, -height/2),
               xy(eye, width/2, height/2), xy(eye, -width/2, height/2)]
        draw.line(pts + [pts[0]], fill=(28, 28, 36, 205), width=round(thickness * scale), joint='curve')
    # Narrow bridge and outward temples.
    bridge_left = xy(left, sep*.43, 0)
    bridge_right = xy(right, -sep*.43, 0)
    draw.line([bridge_left, bridge_right], fill=(28, 28, 36, 205), width=round(thickness * scale))
    draw.line([xy(left, -sep*.42, -.08*sep), xy(left, -sep*.77, -.17*sep)],
              fill=(28, 28, 36, 200), width=round(thickness*scale))
    draw.line([xy(right, sep*.42, -.08*sep), xy(right, sep*.77, -.17*sep)],
              fill=(28, 28, 36, 200), width=round(thickness*scale))
    # Downsample alpha-anti-aliased mask, then losslessly encode entire result.
    overlay = overlay.resize((w, h), resample=Image.Resampling.LANCZOS)
    mask = np.asarray(overlay.getchannel('A')) > 0
    if not 5 < int(mask.sum()) < .015*w*h:
        raise AssertionError('Repair mask exceeds small eye-region limit')
    result = Image.alpha_composite(source.convert('RGBA'), overlay).convert('RGB')
    old = np.asarray(source)
    new = np.asarray(result)
    if not np.array_equal(old[~mask], new[~mask]) or not np.any(old[mask] != new[mask]):
        raise AssertionError('Outside changed or overlay empty')
    return result, mask


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--scene', choices=['038','039'], required=True)
    ap.add_argument('--source', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    ap.add_argument('--source-blob', required=True, help='Pinned GitHub source git blob SHA')
    ap.add_argument('--left-eye', type=point, required=True)
    ap.add_argument('--right-eye', type=point, required=True)
    args=ap.parse_args()
    if args.source.resolve() == args.output.resolve():
        ap.error('Never overwrite original')
    data=args.source.read_bytes()
    actual=git_blob_sha(data)
    if actual != args.source_blob:
        raise ValueError('Source blob mismatch: expected '+args.source_blob+' got '+actual)
    with Image.open(args.source) as opened:
        output, mask = add_glasses(opened.convert('RGB'), args.left_eye, args.right_eye)
        original=np.asarray(opened.convert('RGB'))
    args.output.parent.mkdir(parents=True,exist_ok=True)
    output.save(args.output, 'WEBP', lossless=True, method=6)
    with Image.open(args.output) as check:
        roundtrip=np.asarray(check.convert('RGB'))
    if not np.array_equal(roundtrip, np.asarray(output)) or not np.array_equal(roundtrip[~mask], original[~mask]):
        args.output.unlink(missing_ok=True)
        raise AssertionError('Pixel-exact outside-region verification failed')
    print('Exterior pixel equality: PASS (100%)')
    print('Changed mask pixels:',int(mask.sum()))
    print('Output SHA256:',hashlib.sha256(args.output.read_bytes()).hexdigest())
    print('QA status: PENDING_MANUAL_LOCKED_V2_FACE_REVIEW; no auto PASS')


if __name__=='__main__':
    main()
