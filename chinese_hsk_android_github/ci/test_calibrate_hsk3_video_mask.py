#!/usr/bin/env python3
import unittest
import numpy as np
from calibrate_hsk3_video_mask import quad_validation, inner_mask, SIZE, BBOX, git_blob_sha

class CalibrationTests(unittest.TestCase):
    def setUp(self):
        self.q=np.array([[440,930],[650,932],[645,1060],[425,1065]],dtype=np.float32)
    def test_valid_sc047_laptop(self):
        self.assertEqual(quad_validation(self.q,*SIZE,BBOX['047']).shape,(4,2))
    def test_reject_outside_review_box(self):
        q=self.q.copy();q[0]=[50,50]
        with self.assertRaises(ValueError):quad_validation(q,*SIZE,BBOX['047'])
    def test_reject_out_of_order(self):
        with self.assertRaises(ValueError):quad_validation(self.q[[1,0,2,3]],*SIZE)
    def test_reject_degenerate(self):
        with self.assertRaises(ValueError):quad_validation(np.zeros((4,2)),*SIZE)
    def test_mask_nonempty_and_bounded(self):
        m=inner_mask(SIZE,self.q)
        self.assertTrue(m.sum()>1000)
        self.assertFalse(m[900,200])
        self.assertTrue(m[1000,520])
    def test_blob_sha_known_fixture(self):
        self.assertEqual(git_blob_sha(b'hello'), 'b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0')

if __name__=='__main__':unittest.main(verbosity=2)
