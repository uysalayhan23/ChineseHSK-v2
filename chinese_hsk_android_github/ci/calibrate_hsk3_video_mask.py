#!/usr/bin/env python3
"""Interactive offline video-display quadrilateral calibration, HSK3_SC037/047.

No image edits: verify pinned bytes, click four inner corners in target image
then four inner corners in accepted SC029 donor. Saves a draft JSON +
annotated diagnostic previews. Not a QA PASS or automatic repair.

Usage:
 python calibrate_hsk3_video_mask.py --scene 037 --source SC037.webp \
   --donor SC029.webp --out-dir /tmp/sc037-mask

Requires: opencv-python, numpy, Pillow. Needs a desktop GUI for click selection.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

EXPECTED_BLOBS = {
    "037": "a5cb9965cc996e7451055e76172e6c6e675b9f60",
    "047": "ba337c7c64bed0977bb5ab2942029eab08f88704",
}
DONOR_SHA = "f80a89a4c44243e056a27949e56ebab3ed7694a4"
SIZE = (941, 1672)  # width, height; original app assets
BBOX = {"037": (390, 835, 704, 1140), "047": (400, 892, 705, 1156)}


def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def verified_image(path: Path, expected: str) -> np.ndarray:
    data = path.read_bytes()
    actual = git_blob_sha(data)
    if actual != expected:
        raise ValueError(f"Unexpected Git blob SHA for {path}: {actual}; required {expected}")
    if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise ValueError("Source must be original WebP")
    im = Image.open(path).convert("RGB")
    if im.size != SIZE:
        raise ValueError(f"Wrong dimensions: {im.size}; required {SIZE}")
    return np.asarray(im)


def quad_validation(points: np.ndarray, width: int, height: int, bbox=None):
    q = np.asarray(points, dtype=np.float32)
    if q.shape != (4,2) or not np.isfinite(q).all():
        raise ValueError("Required four finite TL, TR, BR, BL points")
    if np.any(q[:, 0] < 0) or np.any(q[:, 0] >= width) or np.any(q[:, 1] < 0) or np.any(q[:, 1] >= height):
        raise ValueError("Quad extends outside image")
    if bbox is not None:
        x0,y0,x1,y1 = bbox
        if not np.all((q[:,0]>=x0)&(q[:,0]<=x1)&(q[:,1]>=y0)&(q[:,1]<=y1)):
            raise ValueError("Target corners outside SC037/047 laptop review area")
    signed = cv2.contourArea(q, oriented=True)
    if abs(signed) < 1000 or abs(signed) > width*height*.12:
        raise ValueError("Quad too small or too large for a laptop screen")
    if not cv2.isContourConvex(np.rint(q).astype(np.int32)):
        raise ValueError("Corners must form convex quadrilateral TL/TR/BR/BL")
    if np.linalg.norm(q[0]-q[1]) < 15 or np.linalg.norm(q[1]-q[2]) < 15:
        raise ValueError("Edges too short")
    if not (q[0, 0] < q[1, 0] and q[3, 0] < q[2, 0] and q[0, 1] < q[3, 1] and q[1, 1] < q[2, 1]):
        raise ValueError("Corners must be in TL,TR,BR,BL order")
    return q


def inner_mask(size, quad):
    w,h = size
    mask = np.zeros((h,w),np.uint8)
    cv2.fillConvexPoly(mask,np.rint(quad).astype(np.int32),255)
    return cv2.erode(mask,np.ones((3,3),np.uint8),iterations=1)>0


def select_quad(image_rgb: np.ndarray, title: str, scale: float = .65) -> np.ndarray:
    """Click inner TL,TR,BR,BL; r=reset, ENTER=confirm, ESC=cancel."""
    selected=[]
    h,w = image_rgb.shape[:2]
    preview=cv2.resize(image_rgb,(round(w*scale),round(h*scale)))
    original=cv2.cvtColor(preview,cv2.COLOR_RGB2BGR)
    def click(event,x,y,flags,param):
        if event==cv2.EVENT_LBUTTONDOWN and len(selected)<4:
            selected.append((min(round(x/scale),w-1),min(round(y/scale),h-1)))
    cv2.namedWindow(title,cv2.WINDOW_NORMAL)
    cv2.setMouseCallback(title,click)
    try:
        while True:
            layer=original.copy()
            for i,(x,y) in enumerate(selected):
                pt=(round(x*scale),round(y*scale))
                cv2.circle(layer,pt,5,(0,255,255),-1)
                cv2.putText(layer,str(i+1),pt,cv2.FONT_HERSHEY_SIMPLEX,.6,(0,255,255),2)
            if len(selected)==4:
                cv2.polylines(layer,[np.rint(np.array(selected)*scale).astype(np.int32)],True,(0,255,255),2)
            cv2.putText(layer,f"Click TL TR BR BL ({len(selected)}/4); r reset; ENTER confirm",(8,25),cv2.FONT_HERSHEY_SIMPLEX,.45,(0,0,0),2)
            cv2.imshow(title,layer)
            key=cv2.waitKey(60)&255
            if key==27: raise RuntimeError("Calibration cancelled")
            if key==ord('r'): selected.clear()
            if key in (10,13) and len(selected)==4: return np.asarray(selected,dtype=np.float32)
    finally:
        cv2.destroyWindow(title)


def write_draft(scene: str, source: Path, donor: Path, out: Path,
                src_image: np.ndarray, donor_image: np.ndarray,
                dst_pts: np.ndarray, src_pts: np.ndarray):
    dst=quad_validation(dst_pts,*SIZE,BBOX[scene])
    src=quad_validation(src_pts,*SIZE)
    mask=inner_mask(SIZE,dst)
    if np.count_nonzero(mask) >= SIZE[0]*SIZE[1]*.12:
        raise ValueError("Mask is too large")
    out.mkdir(parents=True,exist_ok=True)
    record={"sceneId":f"ZH_HSK3_SC{scene}","targetGitBlobSha":EXPECTED_BLOBS[scene],
            "donorSceneId":"ZH_HSK3_SC029","donorGitBlobSha":DONOR_SHA,
            "targetQuadPx":dst.tolist(),"donorQuadPx":src.tolist(),
            "maskPixelCount":int(mask.sum()),"maskRequiresManualReview":True,
            "visualQa":"NOT_RUN","pixelIntegrity":"NOT_RUN","passAssigned":False,
            "status":"DRAFT_FOR_VISUAL_APPROVAL_ONLY_NO_IMAGE_EDIT"}
    (out/f"ZH_HSK3_SC{scene}_quad_DRAFT.json").write_text(json.dumps(record,indent=2)+"\n",encoding="utf8")
    def annotate(image,quad):
        canvas=image.copy()
        cv2.polylines(canvas,[np.rint(quad).astype(np.int32)],True,(255,255,0),3)
        for idx,point in enumerate(quad):
            pos=tuple(np.rint(point).astype(int))
            cv2.circle(canvas,pos,6,(255,255,0),-1)
            cv2.putText(canvas,str(idx+1),pos,cv2.FONT_HERSHEY_SIMPLEX,.6,(255,255,0),2)
        return Image.fromarray(canvas)
    annotate(src_image,dst).save(out/f"ZH_HSK3_SC{scene}_target_mask_DRAFT.png")
    annotate(donor_image,src).save(out/"ZH_HSK3_SC029_donor_mask_DRAFT.png")
    print("MASK_CALIBRATION_DRAFT_ONLY",out)
    return record


def main():
    p=argparse.ArgumentParser(description=__doc__,formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--scene',choices=['037','047'],required=True)
    p.add_argument('--source',type=Path,required=True)
    p.add_argument('--donor',type=Path,required=True)
    p.add_argument('--out-dir',type=Path,required=True)
    p.add_argument('--scale',type=float,default=.65)
    args=p.parse_args()
    if not(.2<=args.scale<=1):p.error('--scale must be 0.2..1')
    origin=verified_image(args.source,EXPECTED_BLOBS[args.scene])
    donor=verified_image(args.donor,DONOR_SHA)
    t=select_quad(origin,'Target laptop inner display',args.scale)
    d=select_quad(donor,'Accepted SC029 video-feed region (exclude UI)',args.scale)
    write_draft(args.scene,args.source,args.donor,args.out_dir,origin,donor,t,d)


if __name__=='__main__': main()
