# HSK2 SC023 / SC025 — LOCKED_V2 kesin görsel düzeltme brifi

2026-10-10 · Kaynak: kesin `ci/dialogue_final_v5/HSK2/` JSON ve sahne metadata. Bu belge **üretim yönlendirmesidir; QA PASS değildir**.

## ZH_HSK2_SC023 — Liu Mei'nin ilk pasta siparişi

- Mekan: `LOC_ZH_NEW_HOME_001`, evde pasta çalışma/sipariş masası.
- **Yalnız üç yetişkin**: Liu Mei (刘梅), yetişkin komşu (邻居), gözlüklü Zhang Wei (张伟). Çocuk, kedi, ekstra kişi yok.
- Eylem: komşu Liu Mei'ye özel pasta siparişini anlatıyor, Liu Mei tasarım seçeneklerini gösteriyor, Zhang Wei destek oluyor.
- Nesneler: özel pasta örnekleri, pasta tasarım çizimleri, sipariş not defteri, çalışma masası.
- Eski aday: `ZH_HSK2_SC023_candidate.webp` SHA256 `3f7726c8665fa6a37e60cd55d920b567943ed4d2f149b766ac77f4c37254ad14`; **REJECTED**, aile sofrası ve iki çocuk. Bu dosyayı authoritative assete yükleme.

## ZH_HSK2_SC025 — Li Chen ile çay

- Mekan: `LOC_ZH_TEA_HOUSE_001`, geleneksel çay evi.
- **Yalnız üç yetişkin**: gözlüklü Zhang Wei (张伟), **ince koyu dikdörtgen gözlüklü Li Chen (李晨)**, Liu Mei (刘梅).
- Eylem: yeni şehirde yaşam üzerine sıcak, sakin sohbet.
- Nesneler: çaydanlık, çay fincanları, çay evi masası, geleneksel çay evi dekorasyonu.
- Eski aday: `ZH_HSK2_SC025_candidate.webp` SHA256 `0149a2f85734e6446b5943d1b7d0e60ec3c13754ecf7ff3879d8c0558a1a5cc6`; **REJECTED**, evde iki kişi çalışıyor, doğru kadro ve konum yok. Bu dosyayı authoritative assete yükleme.

## Kilitli ortak kurallar ve kapanış kapısı

- HSK1_SC001 warm cinematic 2.5D/3D CGI, tek sahne 9:16, metinsiz/logo/UI/kolajsız.
- HSK2_SC022→023→024 ve HSK2_SC024→025→026 continuity, HSK1_SC001 character identity.
- Yeni doğru görseli 941×1672 veya manifestle uyumlu dikey WebP olarak kaydet; fiziksel dosyayı aç ve SHA256 hesapla.
- Önce app authoritative WebP, sonra runtime mirror **aynı Git blob SHA**; metadata image hash/QA, batch ve queue birlikte güncellenir.
- Yeni aday kadro/mekan/nesne QA veya mirror kontrolünden kalırsa `FAIL/CORRECTION_REQUIRED` korunur.
- GitHub Actions çalıştırma, commit mesajında `[skip ci]` kullan.
