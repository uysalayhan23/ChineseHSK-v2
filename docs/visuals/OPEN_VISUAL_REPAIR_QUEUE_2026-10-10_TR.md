# ChineseHSK-v2 — LOCKED_V2 açık görsel düzeltme kuyruğu

Kaynak: `chinese_hsk_android_github/visual_sources/visual_batch_status.json` (2026-10-10 mevcut denetim sonuçları). Bu dosya iş sırasını belgeler; QA PASS/asset üretim iddiası değildir. `HSK_VISUAL_PRODUCTION_MANIFEST_LOCKED_TR.md`, sahne JSON, karakter/olgunluk kilitleri ve HSK1_SC001 stil referansı her zaman önceliklidir.

## Kurallar

- Yalnız `uysalayhan23/ChineseHSK-v2` `main`, GitHub Actions **çalıştırma**, commitlerde `[skip ci]`.
- Her sahneye özgü doğru kadro, canonical yüz/yaş/gözlük, location, tek sahne 9:16, yazısız/logosuz warm CGI.
- Üretilmiş ve doğrulanmış yeni WebP olmadan `CORRECTION_REQUIRED` → `PASS` dönüşümü yapma.
- App authoritative binary ve `ci/runtime_visuals` aynasının hash eşitliğini, metadata, scene JSON ve önceki/sonraki continuity doğrula.
- Mevcut doğru görselleri değiştirme; APK build tetikleme.

## Öncelikli sıra

1. HSK1 yanlış kadro sahneleri: SC035, SC036, SC048 (önceki sohbetten kalan açık işler).
2. HSK2 yanlış Lele cinsiyeti / fazla figür / Li Chen karakter sürüklenmesi.
3. HSK3 yanlış kadro, mekan, yazı overlay ve yüz kimliği düzeltmeleri.
4. HSK4 gömülü arayüz simgeleri ve Li Chen gözlüğü.
5. HSK5–HSK6 Li Chen kanonik gözlüğü ve diğer kimlik uyuşmazlıkları.

## Denetim kaydına göre açık sahneler

### HSK1: 3 düzeltme — 3/3 KAPANDI

- [x] `ZH_HSK1_SC035` — 2026-10-10 R3: correct binary + mirror SHA + LOCKED_V2 QA PASS
- [x] `ZH_HSK1_SC036` — 2026-10-10 R3: correct binary + mirror SHA + LOCKED_V2 QA PASS
- [x] `ZH_HSK1_SC048` — 2026-10-10 R3: correct binary + mirror SHA + LOCKED_V2 QA PASS

### HSK2 SC020–SC021 aday QA (2026-10-10 R3)

- `ZH_HSK2_SC020`: ilk 941×1672 WebP adayı (SHA256 `ba0d8a199eb6f9786b15f9239204437587e3e5c1fb2ac7e75e3f3569cb24355f`) doğru dört karakteri/erkek Lele'yi ve sahne nesnelerini içerir; ancak TV üzerinde gömülü seçim arayüzü vardır. İkinci yeni görüntüde TV UI giderilse de Zhang Wei canonical gözlüğü kayboldu. Final kabul edilmedi.
- `ZH_HSK2_SC021`: 941×1672 WebP adayı (SHA256 `da7f2461f01dbc64c077bac532819ae464b736b07e01443b76fe38fd4378de0f`) üç doğru karakter, erkek Lele, kütüphane kartı ve banko içerir. Liu Mei'nin canonical yüz/saç sürekliliği henüz onaylanmadı. Final kabul edilmedi.
- **GitHub app/runtime binary değişmedi; QA `FAIL`, durum `CORRECTION_REQUIRED`.** Görseller geçerli LOCKED_V2 QA tamamlanana kadar kuyruğa açık kalır. Actions/CI kullanılmadı.

### HSK2: 10 düzeltme — 2/10 kapandı, 8 açık

- [x] `ZH_HSK2_SC020` — 2026-10-10 R5: correct WebP SHA256 + Git blob + app/runtime mirror + LOCKED_V2 QA PASS
- [x] `ZH_HSK2_SC021` — 2026-10-10 R5: correct WebP SHA256 + Git blob + app/runtime mirror + LOCKED_V2 QA PASS
- [ ] `ZH_HSK2_SC023` — extra_non_dialogue_prominent_character
- [ ] `ZH_HSK2_SC025` — LiChen_identity_drift_missing_signature_glasses
- [ ] `ZH_HSK2_SC026` — Lele_wrong_gender
- [ ] `ZH_HSK2_SC027` — Lele_wrong_gender
- [ ] `ZH_HSK2_SC030` — Lele_wrong_gender
- [ ] `ZH_HSK2_SC033` — extra_non_dialogue_children
- [ ] `ZH_HSK2_SC034` — Lele_wrong_gender
- [ ] `ZH_HSK2_SC035` — extra_non_dialogue_prominent_characters

### HSK3: 13 düzeltme

- [ ] `ZH_HSK3_SC001` — ZhangWei_identity_drift_missing_signature_glasses
- [ ] `ZH_HSK3_SC003` — LiuMei_missing_replaced_by_Yutong_on_video_call
- [ ] `ZH_HSK3_SC005` — extra_non_dialogue_prominent_character_Yutong
- [ ] `ZH_HSK3_SC016` — visual_location_mismatch_with_scene_JSON
- [ ] `ZH_HSK3_SC017` — visual_location_mismatch_with_scene_JSON
- [ ] `ZH_HSK3_SC019` — graphic_annotation_overlay_in_final_asset
- [ ] `ZH_HSK3_SC029` — exact_cast_mismatch_colleague_missing_Yutong_substituted_on_video
- [ ] `ZH_HSK3_SC030` — LiuMei_missing_Yutong_substituted_on_video
- [ ] `ZH_HSK3_SC037` — LiuMei_missing_Yutong_substituted_on_video
- [ ] `ZH_HSK3_SC038` — LiChen_identity_drift_missing_signature_rectangular_glasses
- [ ] `ZH_HSK3_SC039` — LiChen_identity_drift_missing_signature_rectangular_glasses
- [ ] `ZH_HSK3_SC042` — LiuMei_missing_Yutong_substituted_in_grandmother_kitchen_scene
- [ ] `ZH_HSK3_SC047` — LiuMei_missing_Yutong_substituted_on_video_call

### HSK4: 7 düzeltme

- [ ] `ZH_HSK4_SC014` — embedded_review_ui_and_textlike_content_in_final_asset
- [ ] `ZH_HSK4_SC015` — video_call_ui_icons_embedded_in_final_asset
- [ ] `ZH_HSK4_SC016` — video_call_ui_icons_embedded_in_final_asset
- [ ] `ZH_HSK4_SC034` — LiChen_identity_drift_missing_signature_rectangular_glasses
- [ ] `ZH_HSK4_SC035` — LiChen_identity_drift_missing_signature_rectangular_glasses
- [ ] `ZH_HSK4_SC036` — LiChen_identity_drift_missing_signature_rectangular_glasses
- [ ] `ZH_HSK4_SC047` — LiChen_identity_drift_missing_signature_rectangular_glasses

### HSK5: 5 düzeltme

- [ ] `ZH_HSK5_SC032` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK5_SC033` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK5_SC034` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK5_SC047` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK5_SC049` — scene metadata ve görsel QA kaydına bak

### HSK6: 14 düzeltme

- [ ] `ZH_HSK6_SC009` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC015` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC016` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC019` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC022` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC027` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC029` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC031` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC038` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC039` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC040` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC042` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC047` — scene metadata ve görsel QA kaydına bak
- [ ] `ZH_HSK6_SC049` — scene metadata ve görsel QA kaydına bak

Ek manuel inceleme: `ZH_HSK6_SC012` (otomatik PASS değildir).

## Kapanış kapısı

Her onarım için: scene JSON + character bible + style lock → doğru görsel üretimi/geri yükleme → görsel QA → 1080×1920/9:16, tek sahne ve metinsiz politika → app/runtime WebP hash eşitliği → metadata/queue güncellemesi. Yapılmayan doğrulamayı yapılmış işaretleme.

## 2026-10-10 — HSK1 SC035 / SC036 / SC048 yerel aday görseller

Üç 941×1672 (9:16'e çok yakın) PNG kaynağı gözle incelendi ve yerelde WebP üretildi. Görsel kadroları ve sahne nesneleri LOCKED_V2 koşullarına uygun görünüyor; ancak app asset / runtime mirror GitHub binary push ve son SHA doğrulaması henüz yapılmadığından **bu üç sahne hâlâ CORRECTION_REQUIRED**. Sahne metaverisindeki eski FAIL kayıtlarını PASS'a dönüştürme.

| Scene | Aday WebP adı | Boyut | SHA-256 (yerel) |
|---|---|---:|---|
| ZH_HSK1_SC035 | ZH_HSK1_SC035.webp | 195346 byte | `16d9bcd1a802a51a3770abbbbab9e1af6d83ba3a98495aad0c6c5fe54de1f5fb` |
| ZH_HSK1_SC036 | ZH_HSK1_SC036.webp | 232590 byte | `60a62d7343f6b629468c9a41abf040dfb8e7d64598a47c8cea6e5abb35f15139` |
| ZH_HSK1_SC048 | ZH_HSK1_SC048.webp | 258552 byte | `9edd78a7a69b185ac72ef47d0e43016e453d4cf94b5bfda39361d4c80bd593b6` |

Not: Aday dosyalar çalışma ortamındaki `/mnt/data/` altında oluşturuldu; burada kaydedilen SHA bilgisi tek başına GitHub'a binary yüklendiğinin kanıtı değildir. Önce binary upload, sonra GitHub'da SHA + boyut + karakter QA ve metadata/visual_batch_status kapanışı gerekir. Actions tetikleme; commit `[skip ci]`.

### 2026-10-10 R3 kapanışı

SC035, SC036 ve SC048 için doğru WebP görüntüleri app assetlerine yüklendi, SHA256 yerel aday değerleriyle eşleşti, runtime mirror GitHub'da **aynı Git blob SHA** referansıyla senkronlandı. Canonical kadro/obje/9:16/metinsizlik QA PASS. Actions çalıştırılmadı; build tetiklenmedi. HSK1 bu üç düzeltme bakımından kapandı.

### 2026-10-10 R4 — HSK2_SC020 / SC021 final aday yükleme bekliyor

| Sahne | Hazır WebP | Bayt | Yerel SHA256 | GitHub final |
|---|---|---:|---|---|
| ZH_HSK2_SC020 | ZH_HSK2_SC020.webp | 199356 | `988ec92ec9f2cfae15ccbd6933a646ac93deea0404de19f2a515a17084c5920f` | **PENDING** |
| ZH_HSK2_SC021 | ZH_HSK2_SC021.webp | 201070 | `00739a2798474f10816b12e522c5db95508bb0fb5baa2595e74dad18cb0e697d` | **PENDING** |

Her iki aday 941×1672 ve yerelde açılabilir. SC020: baba Zhang Wei gözlüklü, erkek Lele ve sahne eşyaları mevcut; gömülü TV menüsü yok. SC021: erkek Lele, Liu Mei ve kütüphaneci, kart/kitap/banko/bilgisayar mevcut. **App ve runtime mirror mevcut eski binary'ler olarak korunur.** Tekrar yazılmadan QA PASS işaretlenmez. Yükleme sonrası Git blob + gerçek SHA256, cast/continuity ve mirror eşleşmesi doğrulanmalı. [skip ci]; Actions kullanma.


### 2026-10-10 R5 — HSK2_SC020 ve SC021 binary kapanışı

SC020 ve SC021 için app authoritative WebP'ler GitHub'a kullanıcı tarafından yüklendi; yerel aday Git blob SHA eşleşmeleri doğrulandı. `ci/runtime_visuals` aynaları **aynı blob SHA** ile güncellendi. Her iki sahne konuşmacı/kadro ve nesne kontrollü LOCKED_V2 QA PASS; Lele erkek, SC020 Zhang Wei gözlüklü ve UI'siz. Meta/visual_batch_status kapanışı yapıldı, diğer 8 HSK2 onarım açık. Actions kullanılmadı, APK build tetiklenmedi. R4 öncesi PENDING notları tarihsel kayıt niteliğindedir; en güncel sonuç R5'tir.
