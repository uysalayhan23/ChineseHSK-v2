# HSK3 kalan beş görsel — hedefli onarım denetimi (13.10.2026)

**Repo:** `uysalayhan23/ChineseHSK-v2`, `main` · **Kapsam:** SC037, SC038, SC039, SC042, SC047

**Teknik QA:** 44/50 PASS. SC001'in açık kullanıcı onaylı pasta teslim yönü istisnası korunur, teknik PASS'a çevrilmez. SC029 ve SC030 PASS görselleri değiştirilmez. Bu inceleme **görsel onarım değildir** ve yeni bir görsel PASS verilmez.

## Canonical referanslar (GitHub'dan gerçekten doğrulandı)

- **Li Chen** `ci/runtime_characters/CHR_ZH_LICHEN_001/base.webp`, Git blob `9dac437ff1eb857d5aae44cb15239ac882067851`; ince koyu dikdörtgen gözlük, yana taranmış koyu saç, koyu dış kıyafet. Kaynak: `ci/core_character_canon_v1.json` ve karakter promptu.
- **Liu Mei** `ci/runtime_characters/CHR_ZH_LIUMEI_001/base.webp`, Git blob `e6d9790e5dca87b09938bbd496cda57c71e11e9b`; HSK3 olgun yetişkin görünümü, toplu/yarı toplu koyu saç, krem/bej cardigan ve muted yeşil bluz.
- **Onaylı uzaktan Liu Mei** `ZH_HSK3_SC029.webp`, blob `f80a89a4c44243e056a27949e56ebab3ed7694a4` (PASS). `SC030` da PASS, blob `5ec728e434a0a9c08f42ad5d6cfcb57c3d2324de`.
- HSK3 olgunluk kilidi: `docs/visuals/HSK3_CHARACTER_MATURITY_LOCK_TR.md`.

## Hedefli bölgeler

| Sahne | Orijinal Git blob SHA | Değişmesi gereken tek içerik | Kaba inceleme alanı (x1,y1,x2,y2) | Uygulanabilirlik |
|---|---|---|---|---|
| ZH_HSK3_SC037 | `a5cb9965cc996e7451055e76172e6c6e675b9f60` | Liu Mei absent; Yutong-like video participant | `405,855,674,1120` | Yalnız video alanı içine perspektif onarımı |
| ZH_HSK3_SC038 | `5e3745c924c9bf21358b48c29d8236b29795a43a` | Li Chen at far right has no thin dark rectangular glasses | `690,415,932,660` | Yalnız kalibre göz merkezleriyle gözlük katmanı |
| ZH_HSK3_SC039 | `ffbac7f7f84361e3fcfc1b53a0dd47fdd7847e32` | Li Chen at far right lacks signature rectangular glasses | `700,412,932,705` | Yalnız kalibre göz merkezleriyle gözlük katmanı |
| ZH_HSK3_SC042 | `3ff618939e08d4e7d7fe5e4b30878276eb5e56b1` | Far-right Yutong-like young woman substituted for mature Liu Mei | `735,440,940,1040` | **Otomatik onarım güvenli değil:** poz/örtüşme eşleşen yetişkin gövde referansı gerekli |
| ZH_HSK3_SC047 | `ba337c7c64bed0977bb5ab2942029eab08f88704` | Young Yutong-like woman substituted on video call | `421,917,676,1129` | Yalnız video alanı içine perspektif onarımı |

**ÖNEMLİ:** Tablodaki kutular *yalnız inceleme alanıdır*, çalıştırılacak maske veya kalibre edilmiş göz koordinatı değildir. Onarım öncesi orijinal görüntüde çizgi, göz, ekran sınırları ve perspektif elle doğrulanmalıdır. `SC042`'de yüzün yanında saç ve kıyafet de farklı; salt yüz değişimi canonical QA için yeterli olmaz.

## Kabul kapıları

1. Her orijinal WebP Git blob SHA değerini yeniden doğrula; kaynak değişmişse onarımı iptal et.
2. SC037/SC047 için video görüntüsü dışında laptop çerçevesi, çağrı kontrolleri ve fiziksel karakterlere dokunma. SC038/SC039 için sadece gözlük çerçevesi ve köprü çizgilerinin alfa maskesi içinde kal.
3. SC042 için poz ve oklüzyonları tutan canonical Liu Mei kaynak kadrajı bulunana kadar görsel düzeltmesi veya PASS yasak.
4. Kayıpsız WebP çıktı oluştur; çıktıdan yeniden okunmuş RGB verilerinin maske dışında **birebir** piksel eşitliğini denetle.
5. Canonical yüz ve kadro görsel QA'sı PASS olmadan dosya paylaşma/yükleme. Sonra app/runtime mirror aynı blob, metadata ve batch tutarlı olacak şekilde tek `[skip ci]` commit uygula. Actions başlatma.

## İkili aktarım sınırı

GitHub connector Base64 WebP okuma ve Git blob oluşturmayı destekliyor; ancak connector'dan yerel Pillow/OpenCV ortamına doğrudan dosya aktarım yolu **doğrulanmadı**. Önceki doğrudan ağ denemeleri başarısız olduğundan aynı yöntemi tekrarlayıp PASS iddiasında bulunmayacağız. `repair_hsk3_sc037_screen.py`, `repair_hsk3_lichen_glasses.py` ve `repair_hsk3_sc047_fetch_and_run.py` test/altyapı araçlarıdır; test PASS gerçek sahne QA PASS değildir.

**Bu değişiklik yalnız onarım planı ve kaynak hash sabitlemesidir. İkili WebP değiştirilmedi; 44/50 teknik QA sabit.**
