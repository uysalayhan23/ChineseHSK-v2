# HSK2 SC023 / SC025 — R10 LOCKED_V2 final acceptance

Date: 2026-10-11. Repo: `uysalayhan23/ChineseHSK-v2`, branch `main`.

| Scene | Source content | Dimensions | Bytes | SHA256 | Git Blob SHA | QA |
|---|---|---|---:|---|---|---|
| ZH_HSK2_SC023 | 刘梅, 邻居, 张伟 | 941×1672 | 256576 | `73d4cdae2b17a0a496751c2d0d9fb4f32a3c3476cb1641e63ca31ff8ade5c5f3` | `6d4534bed74f52310a9cc8f05ea92a5a97603d05` | PASS |
| ZH_HSK2_SC025 | 张伟, 李晨, 刘梅 | 941×1672 | 233444 | `3d889c1884c03ced8b30e917edfea2c73f8165e449afe8aeee40458bfce9e383` | `8f8dcf66f069a1712fbb78991c22a90186bb4b81` | PASS |

Checks: per-scene dialogue cast, locations, props, canonical HSK1_SC001 / HSK2_SC024 warm 2.5D/3D visual style, no extras, no text/collage, male glasses constraints, app-authoritative WebP and CI runtime WebP identical Git blobs. These are manually reviewed visual-quality acceptances; not automated face-embedding identity proof.

Prior R6/R8 rejections were for **older, different candidates** and remain part of the provenance audit; current R10 candidates have new blobs and final acceptance statuses.

The binary payload was transferred from container to GitHub via an intermediate Base64 text bridge; the resulting Git blob SHA values matched the locally calculated SHA before adding either blob to the tree. No GitHub Actions were triggered manually. Commit message uses `[skip ci]`.
