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

### HSK2: 10 düzeltme — 7/10 kapandı, 3 açık

- [x] `ZH_HSK2_SC020` — 2026-10-10 R5: correct WebP SHA256 + Git blob + app/runtime mirror + LOCKED_V2 QA PASS
- [x] `ZH_HSK2_SC021` — 2026-10-10 R5: correct WebP SHA256 + Git blob + app/runtime mirror + LOCKED_V2 QA PASS
- [x] `ZH_HSK2_SC023` — 2026-10-11 R10: exact cast + cake order context + canonical QA + SHA/mirror PASS
- [x] `ZH_HSK2_SC025` — 2026-10-11 R10: exact cast + rectangular-glasses Li Chen + teahouse + SHA/mirror PASS
- [x] `ZH_HSK2_SC026` — 2026-10-11 R12: ERKEK Lele + gözlüklü Zhang Wei + dört kişilik yağmurlu sabah, Git blob / app-mirror / LOCKED_V2 QA PASS
- [x] `ZH_HSK2_SC027` — 2026-10-11 R13: erkek Lele, mendilli Liu Mei, gözlüklü Zhang Wei, ev ve süt dişi; Git blob / app-mirror / LOCKED_V2 QA PASS
- [x] `ZH_HSK2_SC030` — 2026-10-11 R14: erkek Lele + gözlüklü Zhang Wei, tam 4 kişi, banka / postane / fıskiye, Git blob / app-mirror / LOCKED_V2 QA PASS
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

### 2026-10-10 R6 — HSK2_SC023 ve SC025 yeniden QA / aday reddi

Kaynak diyaloglar: `ci/dialogue_final_v5/HSK2/ZH_HSK2_SC023.json` ve `...SC025.json`. Aday görselleri WebP'ye çevrildi ve SHA256 alındı, ancak LOCKED_V2 kadro/konu koşullarına uymadıkları için **ikisi de REDDEDİLDİ**. 

- [ ] `ZH_HSK2_SC023`: yalnız Liu Mei (刘梅), yetişkin komşu (邻居), Zhang Wei (张伟) ve evde özel pasta siparişi — önceki aday aile yemeği + iki çocuk içeriyor, pasta tasarımları/sipariş not defteri yok. Aday SHA256 `3f7726c8665fa6a37e60cd55d920b567943ed4d2f149b766ac77f4c37254ad14` (941×1672, 206380 bayt).
- [ ] `ZH_HSK2_SC025`: yalnız Zhang Wei (张伟), koyu dikdörtgen gözlüklü Li Chen (李晨), Liu Mei (刘梅), geleneksel çay evi, çaydanlık ve fincanlar — önceki aday ev ortamında iki kişilik ders sahnesi. Aday SHA256 `0149a2f85734e6446b5943d1b7d0e60ec3c13754ecf7ff3879d8c0558a1a5cc6` (941×1672, 146832 bayt).

**App authoritative WebP ve `ci/runtime_visuals` eski eşleşen binary durumunda bırakıldı.** PASS yok, mirror yeniden üretimi yok; Actions çalıştırılmadı. Yeni görseller önce bu sahne brifleriyle üretilmeli ve kesin kadro, görsel, canonical continuity, SHA256 ve mirror testlerini geçmelidir.

### 2026-10-11 R7 — SC023/SC025 yeni WebP adayları hazır, binary aktarımı bekleniyor

R6'da reddedilen aile yemeği/iki kişilik çalışma görselleri ile karıştırılmamalı. Yeni **gerçek dosyalar** aşağıdadır; app authoritative path henüz değiştirilmemiştir.

| Sahne | Yeni hedef WebP | Bayt | SHA256 | Beklenen Git blob SHA |
|---|---|---:|---|---|
| ZH_HSK2_SC023 | `ZH_HSK2_SC023.webp` | 246390 | `cdf7b36aa767bc4520b46a731b9afb05f738c2b9d635e9f03d6beb88593afb2b` | `0d2d6b22b66b34d95d43ca07f5d1049b302ef20e` |
| ZH_HSK2_SC025 | `ZH_HSK2_SC025.webp` | 238846 | `08db1212d5d41b73e894fd0b5f3e39731f80d5ccf57ca456f0b193b574c2e2db` | `1cb4839f11de8e0b9613e4f3ab2024cecb0a9344` |

Her ikisi 941×1672, 3 yetişkin, doğru sahne bağlamı ve kritik objeler için görsel ön kontrolü geçti. **Final LOCKED_V2 QA ve metadata PASS yapılmadı:** fiziksel binary GitHub app kaynağına yüklendikten sonra SHA256/Git blob ve canonical yüz kimliği tekrar denetlenmeli, ancak sonra aynı blob `ci/runtime_visuals` aynasına alınmalı. Kalan HSK2 onarımları 8/10 olarak açık. GitHub Actions çalıştırılmadı; bütün commitlerde `[skip ci]`.

### 2026-10-11 R8 — SC023 / SC025 yükleme sonrası canonical QA

- SC023 ve SC025 WebP dosyaları GitHub `main` dalına kullanıcı tarafından yüklendi; Git blob SHA ve yerel aday SHA256 eşleşmesi doğrulandı.
- SC023: doğru pasta siparişi kadrosu (Liu Mei, komşu, Zhang Wei), gerekli nesneler görünür. Ancak HSK1_SC001/HSK2_SC024 referanslarına göre **canonical yüz ve CGI stil sürekliliği zayıf**; stil/yüz drift nedeniyle FAIL.
- SC025: doğru üç yetişkin (Zhang Wei, Li Chen, Liu Mei), çay evi, ince gözlük, çaydanlık/fincanlar mevcut. Ancak yüz/saç ve photoreal CGI işçiliği canonical 3D stil referanslarıyla eşleşmiyor; FAIL.
- Uygun olmayan dosyaları kullanıcının kaynak repodaki geçmiş commitinde koruyarak `app/src/main/assets/chinese_course/media/scenes` içindeki iki asset önceki mirror Git bloblarına geri döndürüldü. `ci/runtime_visuals` eski blobları korunuyor; app/runtime tekrar eşleşiyor.
- QA `FAIL / CORRECTION_REQUIRED` kaldı; HSK2 onarım 2/10 kapanmış, **8 açık**. GitHub Actions kullanılmadı, commit `[skip ci]`.


### 2026-10-11 R9 — Yeni SC023/SC025 adaylarının checksum kaydı

GitHub bağlantısında **yazma/commit izni aktif**; ancak son üretilen yerel binary'ler GitHub'a yüklenmiş değil. Önceki R8 reddedilen adaylarla karıştırma:

- `ZH_HSK2_SC023`: yeni 941×1672 WebP, 256576 bayt, SHA256 `73d4cdae2b17a0a496751c2d0d9fb4f32a3c3476cb1641e63ca31ff8ade5c5f3`, beklenen Git blob `6d4534bed74f52310a9cc8f05ea92a5a97603d05`. Aday QA/kimlik kapısı **PENDING**.
- `ZH_HSK2_SC025`: yeni 941×1672 WebP, 233444 bayt, SHA256 `3d889c1884c03ced8b30e917edfea2c73f8165e449afe8aeee40458bfce9e383`, beklenen Git blob `8f8dcf66f069a1712fbb78991c22a90186bb4b81`. Aday QA/kimlik kapısı **PENDING**.

Güncel sahne durumu halen `FAIL/CORRECTION_REQUIRED`. `ci/runtime_visuals` değiştirilmedi, önceki app/runtime eşleşmesi bozulmadı. GitHub'a gerçekten aktarım yapılmadıkça PASS yazılmamalı. Actions kullanılmadı; bu yalnız doğru aday checksum'larını kaydeden `[skip ci]` metaveri commitidir.

### 2026-10-11 R10 — SC023 ve SC025 binary transfer ve LOCKED_V2 QA kabulü

**R9 bekleme durumu kapandı.** İki gerçek 941×1672 WebP binary GitHub Git Blob API ile yüklendi. App authoritative asset + `ci/runtime_visuals` aynı, doğrulanmış blob SHA ile eşlendi. LOCKED_V2 kadro, sahne, kritikleri ve HSK1_SC001/HSK2_SC024 stil/kimlik ön kontrolü PASS.

- [x] `ZH_HSK2_SC023`: SHA256 `73d4cdae2b17a0a496751c2d0d9fb4f32a3c3476cb1641e63ca31ff8ade5c5f3` · Git blob `6d4534bed74f52310a9cc8f05ea92a5a97603d05` · 256576 bayt · qaStatus `PASS` · status `FINAL_SOURCE_OVERRIDE`.
- [x] `ZH_HSK2_SC025`: SHA256 `3d889c1884c03ced8b30e917edfea2c73f8165e449afe8aeee40458bfce9e383` · Git blob `8f8dcf66f069a1712fbb78991c22a90186bb4b81` · 233444 bayt · qaStatus `PASS` · status `FINAL_SOURCE_OVERRIDE`.

HSK2 kabul sayısı **44/50**, açık düzeltme **6**: `ZH_HSK2_SC026`, `ZH_HSK2_SC027`, `ZH_HSK2_SC030`, `ZH_HSK2_SC033`, `ZH_HSK2_SC034`, `ZH_HSK2_SC035`. Önceki R6/R8 reddedilen farklı adaylar tarihsel kayıttır; **R10 son karardır.** Actions çalıştırılmadı, committe `[skip ci]`.

### 2026-10-11 R11 — kalan 6 sahnenin kesin üretim brifleri

Mevcut 6 sahne görselleri tekrar tek tek görsel QA ile denetlendi. Ayrıntılı kaynak/kadro/yer/nesne ve düzeltme kuralları: `docs/visuals/HSK2_OPEN_SC026_SC035_EXACT_REPAIR_BRIEFS_R11_2026-10-11_TR.md`.

- SC026, SC027, SC030, SC034: erkek Lele yanlışlıkla kız çizilmiş; Zhang Wei'nin gözlüğü eksik.
- SC033: iki diyalog dışı kız çocuğu var.
- SC035: gereken üç kişi yerine altı kişi var.
- SC026 için iki yeni aday yanlış pasta dükkânı sahnesi ürettiği için reddedildi, GitHub'a eklenmedi.

Görseller henüz değiştirilmedi: **44/50 PASS, 6 açık**. App/runtime korunur; Actions kullanılmadı ve `[skip ci]`.

### 2026-10-11 R12 — HSK2_SC026 WebP gerçek binary kabulü

`ZH_HSK2_SC026.webp` (941×1672, 450862 bayt) GitHub Git Blob API ile aktarılıp hem app authoritative hem `ci/runtime_visuals` yoluna **aynı blob** ile yazıldı. SHA256 `e92b4ef511b43e0aafb14ba998d60c37570dab1c6675c7fd85a424cc313190ad`; Git blob `75d089dd7f5969bd91d941ac9ea25237de775493`.

Sahne: dört kişi yalnızca gözlüklü Zhang Wei, Liu Mei, daha büyük kız Yutong, **erkek Lele**; yağmur, ıslak yol ve okul çantaları. QA `PASS`, metadata `FINAL_SOURCE_OVERRIDE`. Önceki R11 üretim hataları tarihseldir. HSK2 **45/50 PASS**, 5 açık. Actions çalıştırılmadı; `[skip ci]`.

### 2026-10-11 R13 — HSK2_SC027 doğrulanmış WebP QA kabulü

Gerçek `ZH_HSK2_SC027.webp` (941×1672, 247674 bayt) GitHub'a blob olarak aktarıldı. SHA256 `8867030a1d45de631624eb9a3b55c70adbc2066a9f70eeef5ce30630a0013147`, Git blob `3ddc24547fe53190f055084932211d0c39164ea3`. Aynı binary hem app authoritative hem `ci/runtime_visuals` yollarına yazıldı. Yalnız erkek Lele, Liu Mei ve gözlüklü Zhang Wei; düşen süt dişi ve mendil ev oturma alanında. Metadata `PASS / FINAL_SOURCE_OVERRIDE`. HSK2 **46/50 PASS, 4 açık**. Actions çalıştırılmadı, commit `[skip ci]`.

### 2026-10-11 R14 — HSK2_SC030 kasaba gezisi doğrulanmış binary kabulü

Görsel yeniden üretilmedi; daha önce hazırlanan **`güneşli_meydanda_neşeli_aile_yürüyüşü.png`** adayı seçildi. Gerçek WebP `ZH_HSK2_SC030.webp`: 941×1672, 359078 bayt, SHA256 `e43ec115e8d511fffce1a26a48a9c2d6b35453975b23ed59972b92506d80b8c5`; Git blob `7c12d74e0237f9e0a3b2ba2a6c11e7cc5cf72ea4`. App ve `ci/runtime_visuals` aynı Git blob. Dört kişilik doğru aile, erkek Lele, gözlüklü baba, iki farklı kamu binası ve çeşme; yazı/ekstra kişi yok. QA `PASS`, status `FINAL_SOURCE_OVERRIDE`. Toplam HSK2 **47/50 PASS**, 3 açık: SC033, SC034, SC035. Actions çalıştırılmadı; commit `[skip ci]`.
