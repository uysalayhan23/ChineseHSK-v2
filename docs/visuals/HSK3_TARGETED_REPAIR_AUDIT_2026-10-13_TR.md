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


## 13.10.2026 — SC038/SC039 kullanıcı kabulü

Kullanıcı **"Gözlüğü bu şekilde kabul et"** talimatıyla SC038 ve SC039'daki Li Chen'in mevcut gözlük görünümünü aynen kabul etti. Görsel dosyaları değiştirilmez; gözlük maskesi uygulanmaz. Bu, LOCKED_V2 içindeki imza niteliğindeki dikdörtgen gözlük kuralına ilişkin **belgeli kullanıcı istisnasıdır**, otomatik teknik QA PASS değildir.

- SC038 mevcut Git blob: `5e3745c924c9bf21358b48c29d8236b29795a43a`
- SC039 mevcut Git blob: `ffbac7f7f84361e3fcfc1b53a0dd47fdd7847e32`
- Teknik QA toplamı **44/50 PASS** değişmedi; SC001, SC038 ve SC039 kullanıcı kabulüyle korunan 3 istisna.
- Aktif gerçek görsel onarımı gereken yalnız **SC037, SC042, SC047**; yeni görsel ve Actions yok.

## 13.10.2026 — Ekran maskesi kalibrasyon aracı ve SC042 poz donörü incelemesi

- `chinese_hsk_android_github/ci/calibrate_hsk3_video_mask.py`: **yeni görsel üretmez**; orijinal Git blob SHA'sını doğruladıktan sonra SC037/SC047 dizüstü ekranının 4 iç köşesini ve PASS olan SC029 video alanının 4 iç köşesini masaüstü OpenCV penceresinde seçtirir. Dörtgenin boyut/sınır kontrolünü yapar, yalnız onay **taslağı** JSON'u ve tanısal maskeli PNG'ler üretir; hiçbir WebP kaydetmez, PASS atamaz.
- Aynı dizindeki `test_calibrate_hsk3_video_mask.py` sentetik maske geometri testlerinde 6/6 PASS (yerel). Bu test **gerçek** SC037/047 piksel veya canonical QA değildir.
- Mevcut ortamda GitHub connector Base64 dosya içeriği versede yerel Pillow/OpenCV yürütme oturumuna destekli ikili köprü doğrulanamadı; ekran köşeleri gerçek orijinal üzerinde henüz **kalibre edilmedi**. Kaba alanlar otomatik onaylı maskeye yükseltilmedi.
- SC042 için mevcut canonical Liu Mei temel silueti ile PASS olan SC016, SC017, SC041, SC043 ve SC048 görüntüleri görsel olarak karşılaştırıldı. Hiçbiri SC042 sağda ayakta mantı hazırlayan kişinin baş/gövde/el/önlük/tezgâh örtüşmesini aynı pozda vermiyor. SC016 ve SC017 kısmi üst gövde referansları; SC041/043 oturur, SC048 elde kupa. Doğrudan siluet kompoziti güvenli değil, SC042 FAIL korunur.
- SC001/038/039 kullanıcı kabulü, SC029/030 PASS ve HSK3 44/50 teknik QA korunur. Actions başlatılmadı.


## 14.10.2026 — Canonical kaynak ve donor kırpma denetimi

- Kabul edilmiş **SC029** kaynağının 941×1672 yerel kopyası GitHub Git blob `f80a89a4c44243e056a27949e56ebab3ed7694a4` ile doğrulandı. Liu Mei'nin ekran kısmından `(x1=505,y1=679,x2=676,y2=823)` (sağ/alt hariç), 171×144 boyutlu **kayıpsız RGB-piksel-eşdeğer donor kırpması** yapıldı. Kırpma SHA256: `e55fad7d70d263a529ed254135aa4bfba379609d226802c89fa338c2327db6e5`.
- **SC037/SC047:** Referans donor hazır; ancak ilgili hedef WebP'ler piksel düzeyinde yerel ortama aktarılmadı. Hedef ekran çokgeni henüz gerçek kaynakta doğrulanmadı; ekrana müdahale / QA PASS yapılmadı.
- **SC042:** SC041 (blob `3c0eab271f47115d12b1584f1c78d546b1686138`) ve SC043 (blob `9a4e8c4baadad27995664607f8a3873e64e5fcaa`) yetişkin Liu Mei kimliği ve renk paleti için uygundur. Ancak ikisi de **oturur pozda**; SC042 yanlış karakteri ise mutfakta ayakta, elleri hamurda. Canonical base Liu Mei (`e6d9790e5dca87b09938bbd496cda57c71e11e9b`) de poz ve aydınlatmada uyumsuz. **Doğrudan siluet transferi reddedildi.**
- Alternatif `container.download` denemesi yalnız web önceden görülen URL'leri kabul ettiği, GitHub ham WebP URL'si web önbelleğinde açılamadığı için engellendi. Bu **doğrulanmış GitHub→yerel SC037/042/047 ikili transferi değildir**. Aynı başarısız yöntem döngüye alınmayacak.
- Tüm hedef WebP'ler korunur; kullanıcı onaylı SC001, SC038, SC039 istisnaları ve SC029/SC030 PASS kayıtları değişmedi. HSK3 teknik QA **44/50 PASS**, onarım gerektiren **SC037/042/047**. GitHub Actions kullanılmaz.
