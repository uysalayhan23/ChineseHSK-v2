#!/usr/bin/env python3
"""Local, Actions-free integrity tests for repair_hsk3_sc037_screen.py.

These tests are intentionally *not* canonical visual QA of the real SC037 asset.
"""
import hashlib
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from repair_hsk3_sc037_screen import repair


class ScreenOnlyRepairTests(unittest.TestCase):
    def setUp(self):
        rng = np.random.default_rng(37037)
        self.original = rng.integers(0, 256, (128, 96, 3), dtype=np.uint8)
        self.donor = rng.integers(0, 256, (110, 100, 3), dtype=np.uint8)
        self.target = np.array([[15, 20], [75, 18], [72, 52], [18, 56]], dtype=np.float32)
        self.source = np.array([[10, 8], [80, 9], [82, 88], [8, 85]], dtype=np.float32)

    def test_pixel_integrity_outside_mask(self):
        output, mask = repair(self.original, self.donor, self.target, self.source)
        self.assertEqual(mask.dtype, np.dtype('bool'))
        self.assertTrue(np.any(mask))
        self.assertTrue(np.array_equal(output[~mask], self.original[~mask]))
        self.assertTrue(np.any(output[mask] != self.original[mask]))

    def test_original_input_is_not_mutated(self):
        prior = self.original.copy()
        repair(self.original, self.donor, self.target, self.source)
        self.assertTrue(np.array_equal(prior, self.original))

    def test_invalid_coordinates_are_rejected(self):
        quad = self.target.copy()
        quad[0, 0] = -1
        with self.assertRaises(ValueError):
            repair(self.original, self.donor, quad, self.source)

    def test_oversized_mask_is_rejected(self):
        quad = np.array([[1, 1], [94, 1], [94, 125], [1, 125]], dtype=np.float32)
        with self.assertRaises(ValueError):
            repair(self.original, self.donor, quad, self.source)

    def test_lossless_webp_round_trip_remains_exact(self):
        output, mask = repair(self.original, self.donor, self.target, self.source)
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'pixel_integrity_test.webp'
            Image.fromarray(output).save(p, 'WEBP', lossless=True, method=6)
            decoded = np.asarray(Image.open(p).convert('RGB'))
        self.assertTrue(np.array_equal(decoded, output))
        self.assertTrue(np.array_equal(decoded[~mask], self.original[~mask]))


if __name__ == '__main__':
    unittest.main(verbosity=2)
