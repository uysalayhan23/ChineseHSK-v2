# ChineseHSK-v2 HSK2_SC035 R17 — Visual QA blocker & HSK2 closure gate

Date: 2026-10-11. Repository `uysalayhan23/ChineseHSK-v2` main. Canonical authority: `chinese_hsk_android_github/docs/visuals/HSK_VISUAL_PRODUCTION_MANIFEST_LOCKED_TR.md` LOCKED_V2, especially rule 30 requiring one unique final image per scene ID.

## Source truth

Scene: `ZH_HSK2_SC035`, `LOC_ZH_NEW_HOME_001`; exactly 刘梅 (Liu Mei), 邻居 (one adult neighboring customer), 张伟 (Zhang Wei, signature dark glasses). Props: cakes/pastries, kitchen worktable and customer order list. Dialogue 100 lines, explicit `点心` (pastry/snack), `蛋糕` (cake), `订单` (order), sales/customers. This scene is a subsequent cake/order demand episode and should have its own image rather than cloning SC023.

## R17 visual decisions

- Current authoritative SC035 WebP: rejected, extra non-dialogue figures. Preserve app/mirror until an accepted distinct final scene is ready.
- Existing accepted `ZH_HSK2_SC023` would match cast/cakes but is prohibited as the same final image for two scene IDs by locked rule 30. **Do not reuse that Git blob for SC035.**
- Three candidate scenes were generated specifically for this SC035 task; all had three adult speakers, home kitchen, paperwork and steamed dumplings/buns, but lacked cake `蛋糕` and did not fully match the locked dialogue. These images all have **REJECTED** status. No new source WebP blob was attached to main.
- The reported SHA256 identities of the rejected PNGs are `b2e35fac053df7f661ae4c9cfce77835f72307047716634543958c55f172ba32`, `5d02a388fe59e719086256f433acb300e3c48badb3a8224e97be96e0c929a1b3`, `80e0dcb07553c142dce97d9eb4cd442833aec8a90dbe7357f01106c85b8dfa06`. They are not final assets.

## Repository closure audit

| Gate | Result |
|---|---|
| Authoritative HSK2 scene WebP paths present | **50/50** |
| CI/runtime scene mirrors present | **50/50** |
| Source/mirror Git blob mismatches | **0** |
| Duplicate blobs across different authoritative HSK2 scene IDs | **0** |
| Per-scene metadata files present | **50/50** |
| Per-scene metadata individually read | **50/50** |
| Metadata manifests `LOCKED_V2`, scene IDs agree | **50/50** |
| Scenes with final visual QA PASS | **49/50** |
| Scenes with `CORRECTION_REQUIRED` | **SC035** |
| HSK2 visual closure | **BLOCKED** |

No GitHub Actions run, no build approved or triggered. The scene metadata must stay `qaStatus=FAIL`, `status=CORRECTION_REQUIRED` until a separate compliant WebP is QA approved, uploaded, and mirrored to both paths. Commit uses `[skip ci]`.
