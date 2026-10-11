# HSK2_SC023 + SC025 — GitHub upload sonrası LOCKED_V2 canonical QA (R8)

Tarih 2026-10-11. Kesin diyalog `ci/dialogue_final_v5/HSK2` dosyaları, görsel metadata ve referans `ZH_HSK1_SC001.webp` / `ZH_HSK2_SC024.webp` temelinde denetlendi.

| Sahne | Dosya SHA256 | Git blob | Kadro/mekan/nesneler | Canonical yüz/stil | Sonuç |
|---|---|---|---|---|---|
| ZH_HSK2_SC023 | `cdf7b36aa767bc4520b46a731b9afb05f738c2b9d635e9f03d6beb88593afb2b` | `0d2d6b22b66b34d95d43ca07f5d1049b302ef20e` | Uyumlu | Farklı CGI yüz/saç stilizasyonu | **REJECTED** |
| ZH_HSK2_SC025 | `08db1212d5d41b73e894fd0b5f3e39731f80d5ccf57ca456f0b193b574c2e2db` | `1cb4839f11de8e0b9613e4f3ab2024cecb0a9344` | Uyumlu | Farklı CGI yüz/saç stilizasyonu | **REJECTED** |

İki yeni dosya da doğru GitHub app asset path'ine yüklenmiş ve yerel Git blob ID'leriyle eşleşmiştir. SC023 pasta siparişindeki üç yetişkini, SC025 geleneksel çay evindeki üç yetişkini içerse de yüz/saç, yaş algısı ve gerçekçi-stil farkları nedeniyle `LOCKED_V2` kimlik sürekliliği kabulünden geçmemiştir. SC025 Li Chen'in dar dikdörtgen gözlük niteliği tek başına tam yüz kimliği kabulü için yeterli değildir.

**Karar:** QA PASS yapılmadı. Yanlış binary'lerin authoritative runtime'a kalıcı taşınmasını önlemek için `app/src/main/assets/chinese_course/media/scenes/ZH_HSK2_SC023.webp` ve `ZH_HSK2_SC025.webp` önceki Git bloblarına döndürüldü; `ci/runtime_visuals` değişmedi. Böylece app/runtime mirror eşitliği sağlandı. Yeni aday WebP blobları, önceki Git geçmişinde inceleme için korunmuştur.

Yeniden üretim: modelden kabul edilmiş canonical karakter yüzlerini (HSK1_SC001 CGI), karşılıklı görsel stil ve kostümde doğal süreklilikle referans al; tek sahne/9:16/metinsiz/kolajsız. SC023 yetişkin komşu/Liu Mei/gözlüklü Zhang Wei, pasta tasarım çizimleri ve sipariş defteri. SC025 koyu dikdörtgen gözlüklü Li Chen/gözlüklü Zhang Wei/Liu Mei, ahşap Çin çay evi ve demlik/fincanlar. Önce görsel QA, sonra binary aktarımı/metadata QA.

Actions çalıştırılmadı, APK build başlatılmadı. Commit `[skip ci]`.
