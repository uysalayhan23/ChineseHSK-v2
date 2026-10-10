# HSK2_SC020 / SC021 — LOCKED_V2 aday görsel QA R3

- Repo: `uysalayhan23/ChineseHSK-v2` — `main`
- Kesin diyalog: `ci/dialogue_final_v5/HSK2/ZH_HSK2_SC020.json`, `...SC021.json`
- Stil referansı: `ZH_HSK1_SC001.webp` ve canonical character bible
- Sonuç: **Düzeltilmiş adaylar hazır, ancak final QA PASS değil.**

## SC020: film gecesi

Konuşmacılar: 张伟, 刘梅, 张雨桐, 张乐乐. Mekan LOC_ZH_NEW_HOME_001. Film ekranı, uzaktan kumanda, patlamış mısır ve oturma odası koltuğu gerekir. Lele erkek çocuk olmalıdır.

İlk aday 941×1672 WebP, 205250 bayt, SHA256 `ba0d8a199eb6f9786b15f9239204437587e3e5c1fb2ac7e75e3f3569cb24355f`. Karakter sayısı ve Lele cinsiyeti doğru, ancak TV içinde seçim arayüzü bulunuyor. İkinci deneme arayüzü kaldırdı ama 张伟'nin ayırt edici gözlüğünü kaybetti. Hiçbiri final kaynak WebP'ye taşınmadı.

## SC021: kütüphane kartı

Konuşmacılar: 张乐乐, 刘梅, 图书管理员. Mekan LOC_ZH_LIBRARY_001. Kütüphane kartı, kitaplar, banko ve bilgisayar gerekir.

Aday 941×1672 WebP, 188148 bayt, SHA256 `da7f2461f01dbc64c077bac532819ae464b736b07e01443b76fe38fd4378de0f`. Doğru üç kişi ve erkek çocuk Lele var. Ancak Liu Mei'nin yüz/saç identity-lock eşleşmesi kesin doğrulanamadı. Final kaynak WebP'ye taşınmadı.

## Kabul koşulları

1. Exact cast, canonical yüz, gözlük ve saç referanslarıyla gözden geçirilmiş görseller.
2. Sahneye özgü tek 9:16 görsel, üstüne gömülü yazı/arayüz/kolaj olmadan.
3. App source + `ci/runtime_visuals` fiziksel WebP dosyaları aynı Git blob SHA'sına sahip olmalı.
4. Yeni binary GitHub'dan tekrar okunup SHA256, metadata ve visual_batch_status kapanışı onaylanmalı.
5. Actions çalıştırılmadı; CI için bir PASS iddiası yok.
