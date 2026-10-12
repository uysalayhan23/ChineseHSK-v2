#!/usr/bin/env python3
"""Non-generative screen-content repair for HSK3_SC037.

Replaces pixels *strictly inside* a user-specified quadrilateral with a
perspective-warped rectangular donor region. It never modifies anything else.
This tool DOES NOT assign canonical QA PASS; an operator must review the output.

Requires Pillow and OpenCV. Example:
 python repair_hsk3_sc037_screen.py \
   --source ZH_HSK3_SC037.webp --donor ZH_HSK3_SC029.webp \
   --output ZH_HSK3_SC037_repaired.webp \
   --target-quad '425,902;655,895;650,1080;415,1088' \
   --donor-quad '478,683;692,685;692,831;480,831'

Coordinates must be calibrated against ORIGINAL 941x1672 WebP, never a
regenerated scene. Target quad must stay within the laptop display. Donor
quad must include only the accepted video participant, not the bezel/UI.
"""

from __future__ import annotations
import argparse
import hashlib
from pathlib import Path

import cv2
import numpy as np
from PIL import Image


def parse_quad(value: str) -> np.ndarray:
    try:
        pts = [[float(c) for c in p.split(',')] for p in value.split(';')]
        a = np.array(pts, dtype=np.float32)
        assert a.shape == (4, 2)
        return a
    except (ValueError, AssertionError) as e:
        raise argparse.ArgumentTypeError('Quad: TLx,TLy;TRx,TRy;BRx,BRy;BLx,BLy') from e


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def repair(original: np.ndarray, donor: np.ndarray, dst_quad: np.ndarray,
           src_quad: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    height, width = original.shape[:2]
    if original.ndim != 3 or original.shape[2] != 3:
        raise ValueError('Source must be RGB')
    for q, limits in [(dst_quad, (width, height)), (src_quad, (donor.shape[1], donor.shape[0]))]:
        if not np.all(np.isfinite(q)) or np.min(q) < 0 or np.max(q[:, 0]) >= limits[0] or np.max(q[:, 1]) >= limits[1]:
            raise ValueError('Quadrilateral extends outside image')
        if abs(cv2.contourArea(q.astype(np.float32))) < 80:
            raise ValueError('Quadrilateral too small')
    area = cv2.contourArea(dst_quad)
    if area >= width*height*0.18:
        raise ValueError('Target polygon too large; screen-region-only safety check')
    transform = cv2.getPerspectiveTransform(src_quad, dst_quad)
    warped = cv2.warpPerspective(donor, transform, (width, height),
                                 flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE)
    # Erode 1 pixel so the laptop bezel and the entire exterior stay untouched.
    mask = np.zeros((height, width), np.uint8)
    cv2.fillConvexPoly(mask, np.rint(dst_quad).astype(np.int32), 255)
    mask = cv2.erode(mask, np.ones((3,3), np.uint8), iterations=1) > 0
    result = original.copy()
    result[mask] = warped[mask]
    if not np.any(result[mask] != original[mask]):
        raise AssertionError('Nothing changed inside selected screen')
    if np.any(result[~mask] != original[~mask]):
        raise AssertionError('Pixels changed outside selected screen')
    return result, mask


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--source', required=True, type=Path)
    ap.add_argument('--donor', required=True, type=Path)
    ap.add_argument('--output', required=True, type=Path)
    ap.add_argument('--target-quad', required=True, type=parse_quad)
    ap.add_argument('--donor-quad', required=True, type=parse_quad)
    args = ap.parse_args()
    if args.source.resolve() == args.output.resolve():
        ap.error('Never overwrite original scene')
    source = np.asarray(Image.open(args.source).convert('RGB'))
    donor = np.asarray(Image.open(args.donor).convert('RGB'))
    result, mask = repair(source, donor, args.target_quad, args.donor_quad)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Lossless encoding is vital: lossy WebP would perturb untouched pixels.
    Image.fromarray(result).save(args.output, format='WEBP', lossless=True, method=6)
    decoded = np.asarray(Image.open(args.output).convert('RGB'))
    if not np.array_equal(decoded[~mask], source[~mask]):
        args.output.unlink(missing_ok=True)
        raise AssertionError('WebP round-trip changed exterior pixels; refusing output')
    if not np.array_equal(decoded, result):
        args.output.unlink(missing_ok=True)
        raise AssertionError('WebP encoding was not pixel-exact; refusing output')
    print('PIXEL_INTEGRITY_PASS: 100% exterior pixels identical')
    print('DISPLAY_REGION_CHANGED:', int(np.count_nonzero(np.any(decoded[mask] != source[mask], axis=1))))
    print('SOURCE_SHA256:', sha256(args.source))
    print('DONOR_SHA256:', sha256(args.donor))
    print('OUTPUT_SHA256:', sha256(args.output))
    print('QA_STATUS: PENDING_VISUAL_CANONICAL_REVIEW (not PASS)')


if __name__ == '__main__':
    main()
