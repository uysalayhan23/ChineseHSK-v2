"""Unit verification on synthetic RGB pixels, not final canonical visual QA."""
import unittest
import numpy as np
from PIL import Image
from repair_hsk3_lichen_glasses import add_glasses, git_blob_sha

class TestLiChenGlasses(unittest.TestCase):
    def setUp(self):
        self.original=Image.fromarray(np.full((1672,941,3), (209,179,161), np.uint8), 'RGB')

    def test_pixel_integrity(self):
        result, mask=add_glasses(self.original, (735,530),(791,529))
        a=np.asarray(self.original); b=np.asarray(result)
        self.assertTrue(np.array_equal(a[~mask],b[~mask]))
        self.assertGreater(np.count_nonzero(np.any(a[mask]!=b[mask],axis=1)), 0)

    def test_lossless_webp(self):
        import tempfile
        from pathlib import Path
        result,mask=add_glasses(self.original,(780,510),(820,512))
        with tempfile.TemporaryDirectory() as t:
            p=Path(t)/'test.webp'
            result.save(p,'WEBP',lossless=True,method=6)
            b=np.asarray(Image.open(p).convert('RGB'))
        self.assertTrue(np.array_equal(b,np.asarray(result)))
        self.assertTrue(np.array_equal(b[~mask],np.asarray(self.original)[~mask]))

    def test_invalid_eye(self):
        with self.assertRaises(ValueError):
            add_glasses(self.original,(10,10),(600,10))

    def test_geometry_clamp(self):
        with self.assertRaises(ValueError):
            add_glasses(self.original,(-10,10),(50,10))

    def test_distinct_sha(self):
        self.assertNotEqual(git_blob_sha(b'abc'),git_blob_sha(b'abd'))

if __name__=='__main__': unittest.main(verbosity=2)
