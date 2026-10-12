#!/usr/bin/env python3
"""Offline byte-level tests; NOT a visual QA PASS for SC047."""
import base64
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import repair_hsk3_sc047_fetch_and_run as mod


class TestVerifiedBridge(unittest.TestCase):
    def setUp(self):
        self.webp = b'RIFF' + b'\0\0\0\0' + b'WEBP' + b'VP8 ' + b'\0\0\0\0'
        self.sha = mod.git_blob_sha(self.webp)

    def package(self, data=None, sha=None, encoding='base64'):
        raw = self.webp if data is None else data
        return json.dumps({'encoding':encoding,
                           'sha':sha or mod.git_blob_sha(raw),
                           'content':base64.b64encode(raw).decode()}).encode()

    def test_verified_github_blob(self):
        self.assertEqual(mod.decode_github_content(self.package(), self.sha), self.webp)

    def test_wrong_expected_sha_fails(self):
        with self.assertRaises(ValueError):
            mod.decode_github_content(self.package(), '0'*40)

    def test_unexpected_api_sha_fails(self):
        with self.assertRaises(ValueError):
            mod.decode_github_content(self.package(sha='1'*40), self.sha)

    def test_reject_nonwebp_content(self):
        with self.assertRaises(ValueError):
            mod.decode_github_content(self.package(b'arbitrary text'), mod.git_blob_sha(b'arbitrary text'))

    def test_repair_requires_explicit_calibrated_quads(self):
        with tempfile.TemporaryDirectory() as d, self.assertRaises(ValueError):
            mod.run(Path(d))

    def test_prepare_only_never_runs_image_repair(self):
        with tempfile.TemporaryDirectory() as d:
            with patch.object(mod,'get_verified_webp',return_value=self.webp):
                result=mod.run(Path(d),prepare_only=True)
            self.assertIsNone(result)
            for n in ('047','029'):
                self.assertEqual((Path(d)/f'ZH_HSK3_SC{n}_verified_original.webp').read_bytes(),self.webp)
            self.assertFalse((Path(d)/'ZH_HSK3_SC047_repaired_QA_PENDING.webp').exists())


if __name__=='__main__':
    unittest.main(verbosity=2)
