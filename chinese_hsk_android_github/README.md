# Chinese HSK Life Journey — Android / GitHub APK Project

Bu depo, Basitleştirilmiş Çince (简体中文) HSK1–HSK6 kursunu tek Android uygulamasında offline çalıştırmak için hazırlanmıştır.

## Kullanıcı açısından hedef

Kullanıcı JSON, klasör veya sahne dosyalarını elle birleştirmez. İçerik `authoring/` kaynağından derlenir, doğrulanır ve Android `assets` içine otomatik aktarılır. GitHub Actions daha sonra APK üretir.

## v2.0.0 proje durumu

## v2.0.0 — GitHub tek-tık APK ve Sistem Kontrolü

- Dashboard'a **Sistem Kontrolü** ekranı eklendi: kurs bütünlüğü, telefonun Mandarin TTS sesleri, cihaz-içi Mandarin konuşma tanıma, bildirim izni ve yerel ses cache durumu tek ekranda görünür.
- `Build Personal APK` workflow'u kullanıcı dostu hale getirildi. Başarılı build sonunda APK `ChineseHSK-v2.0.0-personal.apk` veya kalıcı signing aktifse `ChineseHSK-v2.0.0-release.apk` adıyla artifact içine konur.
- APK ile birlikte SHA-256 kontrol özeti ve build özeti üretilir.
- Build başarısız olsa bile validation raporları ayrı artifact olarak yüklenir.
- Workflow, GitHub Secrets üzerinden opsiyonel kalıcı release signing destekler; signing secrets yoksa kişisel debug APK üretir. Signing key repoya yazılmaz.
- Basit GitHub kullanım kılavuzu: `docs/GITHUB_APK_BUILD_TR.md`.

- HSK1–HSK6: **300/300 scene blueprint hazır ve 300/300 sahne veri açısından tamdır**.
- HSK1: **50/50 tam**.
- HSK2: **50/50 tam**.
- HSK3: **50/50 tam**.
- HSK4: **50/50 tam**.
- HSK5: **50/50 tam**.
- HSK6: **50/50 tam**.
- Toplam **30.000 diyalog turu** ve **2.400 flash-card** vardır.
- Her tam diyalogda Simplified Chinese + tone-marked Hanyu Pinyin + offline Turkish alanı vardır.
- Her sahnede flash-card, favori çalışma, üç tür cümle alıştırması, anlama, telaffuz, interaktif diyalog ve iki aşamalı sınav verisi bulunur.
- Kelime sınavı **>= %90**, cümle sınavı **>= %85** olmadan sonraki sahne açılmaz.
- `scripts/validate_content.py --strict` artık normal GitHub APK build akışında da zorunludur.
- Teknik veri tamlığı sağlanmıştır; **300 sahnenin tamamı native/editoryal Mandarin incelemesi önerisiyle işaretlidir**. Bu bayrak özellikle HSK5–HSK6'daki doğal kullanım, kültürel/pragmatik nüans ve ticari yayın kalitesi için korunur.


## v1.9.0 çalışma alışkanlığı ve zayıf-kelime sistemi

- Ayarlardan açılıp kapatılabilen günlük offline çalışma hatırlatıcısı vardır.
- Hatırlatma saati cihazda saklanır; WorkManager ile yeniden zamanlanır.
- Android 13+ cihazlarda bildirim izni kullanıcıdan hatırlatıcı açılırken istenir.
- Uygulama çalışma günlerini yalnızca telefonda tutar ve mevcut/en uzun seri ile son 28 günlük takvimi gösterir.
- Kelime sınavında yanlış cevaplanan veya flash-card ekranında **Zor** işaretlenen kelimeler otomatik Zayıf Kelimeler havuzuna girer.
- Günlük tekrar sırası: **zayıf kelimeler → favoriler → son sahne kelimeleri**.
- Zayıf kelimeler ayrı ekranda tekrar çalışılabilir ve doğru tekrarlarla önceliği düşer.
- Bu verilerin tamamı mevcut yerel JSON yedekleme sistemine dahildir.

## Tamamlanan seviyelerde öğrenme akışı

Her sahnede:

1. Hikâye sahnesi (~100 konuşma turu)
2. Sahneye uygun flash-card kelimeler
3. İleri/geri kart navigasyonu
4. Kalıcı favori kelimeler ve ayrı Favorilerim çalışma ekranı
5. Cümle alıştırmaları
   - kelime sıralama
   - boşluk doldurma
   - yanlış sırayı düzeltme
6. Anlama soruları
7. Telaffuz hedefleri
8. İnteraktif diyalog
9. Zorunlu iki aşamalı sınav
   - Aşama 1 Kelime: **>= %90**
   - Aşama 2 Cümle: **>= %85**
10. İki aşama geçilmeden sonraki sahne açılmaz.

Geçilmiş sahneler, kartlar ve sınavlar yeniden çalışılabilir. Favoriler ve en iyi skorlar cihazda offline saklanır.


## v1.4.0 görsel/ses sahne katmanı

- Sahne oynatıcı artık `locationId` + karakter kataloglarını gerçek bir **Visual Novel / Theatre stage** katmanında kullanır.
- Her sahnede aktif konuşan karakter otomatik öne çıkar; aynı sahnedeki diğer karakterler arka planda tutulur.
- 63 mekân için tekrar kullanılabilir görsel tema/fallback tanımı vardır. Gerçek WebP arka plan eklendiğinde otomatik olarak Compose fallback'in yerini alır.
- 83 karakter/rol profili ve 84 ses profili tek katalogda tutulur.
- Karakter portresi yoksa hafif Compose avatarı kullanılır; `portraitAsset` mevcutsa portre otomatik yüklenir.
- Anlatıcı sahne açılışında devreye girer. İlk kez açılan bir sahnede anlatım bittikten sonra diyalog otomatik başlayabilir.
- Ses sistemi **authored audio first → offline Mandarin TTS fallback** olarak çalışır. `DLG_...` Opus dosyası varsa onu, yoksa yerel TTS'yi kullanır.
- Sahne ambience ve müzik dosyaları için de aynı ID tabanlı yuvalar vardır; dosya yoksa sessizce fallback yapılır.
- Pinyin/Türkçe varsayılan görünümü seviyeye göre scene data içindeki `defaultSubtitleMode` alanından alınır (HSK1–2 daha fazla yardım, HSK4–6 daha az).
- Binary görsel/ses dosyaları henüz pakete gömülü değildir; uygulama bu yüzden bozulmaz ve mevcut fallback'lerle tamamen offline çalışır.

### Medya yol sözleşmesi

```text
background  chinese_course/media/backgrounds/<LOCATION_ID>_DAY.webp
character   chinese_course/media/characters/<CHARACTER_ID>/base.webp
dialogue    chinese_course/media/audio/dialogues/<DIALOGUE_ID>.opus
narrator    chinese_course/media/audio/narrator/<SCENE_ID>.opus
ambience    chinese_course/media/ambience/<LOCATION_ID>.opus
music       chinese_course/media/music/<SCENE_ID>.opus
```

`python3 scripts/build_media_catalog.py` bu katalogları temiz checkout'tan yeniden üretir.


## v1.4.0 görsel üretim pipeline'ı

- Görsel üretim artık `visual_sources/` kaynak alanı → otomatik WebP optimizasyonu → Android assets zinciriyle çalışır.
- `scripts/build_visual_asset_queue.py` 93 adet deterministik görsel hedefi üretir: 33 karakter kimlik/yaş varyantı + 60 mekân zaman/hava/dekor varyantı.
- `scripts/prepare_visual_assets.py` kaynak PNG/JPG/WebP görselleri otomatik olarak runtime boyutuna dönüştürür: karakter 768×1152 şeffaf WebP, arka plan 1280×720 WebP.
- `scripts/validate_visual_binaries.py` varsa gerçek görsellerin formatını, boyutunu ve karakterlerde alpha kanalını doğrular.
- `scripts/validate_visual_runtime_mapping.py` ana karakterlerin HSK seviyesine göre doğru yaş varyantını kullanacağını kontrol eder.
- Zhang Wei, Liu Mei, Li Chen, çocuklar, Mimi ve büyükanne/büyükbaba HSK seviyesi ilerledikçe veri tabanlı `portraitByLevel` eşlemesiyle yaşlanır.
- Kaynak görsel henüz yoksa build kırılmaz; Compose fallback + mevcut medya fallback sistemi çalışmaya devam eder.
- `visual_sources/visual_asset_queue.csv` üretim sırasını ve hedef dosya yollarını insan tarafından okunabilir şekilde gösterir.


## v1.5.0 ses üretim ve medya hazırbulunuş pipeline'ı

- 30.000 diyalog repliği için deterministik authored-audio hedef yolu oluşturulur.
- 63 tekrar kullanılabilir ambience ve 300 opsiyonel sahne müzik slotu aynı kuyrukta izlenir.
- `scripts/build_audio_asset_queue.py` mevcut 300 sahneden ses üretim kuyruğunu otomatik türetir.
- Kaynak master sesler `audio_sources/` altında tutulur; `scripts/prepare_audio_assets.py` bunları `ffmpeg` ile runtime Opus formatına dönüştürür.
- Konuşma hedefi: 24 kHz mono 48 kbps Opus; ambience: 48 kHz stereo 64 kbps; müzik: 48 kHz stereo 96 kbps.
- `scripts/validate_audio_assets.py` varsa authored seslerin codec/sample-rate/channel yapısını doğrular.
- Diyaloglar, Android telefonda seçili olan Mandarin TTS sesiyle metinden okunur.
- `MEDIA_READINESS_REPORT_v1.5.0.txt` ve `media/media_readiness.json` her build'de gerçek binary medya sayısını fallback sayısından ayırır.
- Kişisel APK authored binary medya olmadan da çalışabilir; ticari yayın için gerçek ses/görsel kalite kontrolü ayrı tutulur.

## Tek kaynak yaklaşımı

`authoring/` → `scripts/compile_authoring.py` → `app/src/main/assets/chinese_course/` → Android uygulama

HSK1–HSK6 tam diyalogları kendi `authoring/hskX_blueprints.json` dosyalarında tutulur. Böylece GitHub Actions temiz bir checkout'tan projeyi yeniden oluşturduğunda sahne içeriği kaybolmaz.

## Yerel doğrulama

```bash
python3 scripts/compile_authoring.py
python3 scripts/build_media_catalog.py
python3 scripts/validate_content.py
```

Tam kurs final kontrolü:

```bash
python3 scripts/validate_content.py --strict
```

`--strict` ancak 300/300 sahne tam olduğunda geçer.

## GitHub Actions

`Build Debug APK`:
1. Java 17
2. Gradle 9.6
3. Android API 37
4. authoring derleme
5. görsel/ses kataloglarının üretilmesi
6. strict içerik validation + kalite audit
7. debug APK
8. GitHub artifact upload

`Validate Full Course` finalde 300/300 tam sahne şartını uygular.

## Android yapı sürümleri

- Android Gradle Plugin: 9.4.0
- Gradle: 9.6.0
- JDK: 17
- compileSdk / targetSdk: 37
- minSdk: 26
- Compose BOM: 2026.09.00

## İçerik kalite durumu

`complete=true` veri sözleşmesinin eksiksiz olduğunu ifade eder: diyalog sayısı, Pinyin/TR alanları ve zorunlu öğrenme modülleri mevcuttur. `editorialStatus` ise dil doğallığı/native Mandarin gözden geçirmesini ayrıca takip eder. Bu ayrım, eksik dosya ile dil editörlüğünü birbirine karıştırmamak için bilerek kullanılır.

## v1.1.0 Android player

The app now includes a phone-oriented scene player instead of rendering all ~100 dialogue turns as one long list. It supports one active line at a time, previous/next navigation, auto-play, Pinyin/TR toggles, persistent resume position, playback speed, Mandarin TTS fallback, comprehension practice, pronunciation practice (on-device recognizer only), interactive dialogue, flash cards/favorites, sentence practice, and the two-stage locked exam flow.

Pronunciation recognition intentionally refuses a network recognizer fallback. If Android reports no on-device Mandarin recognition service, listening/repetition remains available and the app reports that offline recognition is unavailable on that device.

## v1.3.0 — Görsel Kimlik Sistemi
Ana aile, Li Chen çevresi, büyükanne/büyükbaba ve 15 ana mekân için görsel kimlikler sabitlendi. Ayrıntılar `docs/VISUAL_PRODUCTION_GUIDE_TR.md` ve `app/src/main/assets/chinese_course/media/visual_bible.json` dosyalarındadır. Gerçek WebP dosyaları daha sonra aynı ID yollarına eklendiğinde uygulama otomatik kullanır; eksikken Compose fallback çalışır.


### v1.7.0 onboarding + dashboard + placement

- İlk açılışta **Sıfırdan Başla** veya **Seviyemi Belirle** akışı eklendi.
- 12 soruluk offline placement testi HSK1-HSK6 başlangıç seviyesi önerir.
- Placement sonucu yalnızca önerilen seviyenin ilk sahnesini başlangıç noktası olarak açar; sonraki sahneler yine %90 kelime + %85 cümle sınavı ile açılır.
- Yeni ana dashboard: kaldığın yerden devam, HSK seviyeleri, favoriler, günlük tekrar ve placement testini yenileme.
- Kaldığın son sahne ve replik cihazda saklanır.
- Günlük tekrar ekranı favoriler ile son çalışılan sahnenin kelimelerini birleştirir.

### v1.6.0 offline character voice cache
The app can use locally installed Mandarin Android TTS voices without requiring cloud speech. Each character's stable Voice Bible profile is deterministically mapped to a local offline Mandarin voice. Missing authored dialogue audio is synthesized once to app-private storage and reused later. The cache is automatically capped at roughly 350 MB.

## v1.8.0 — Ayarlar, ilerleme ve yerel yedek
- Dashboard'da **İlerlemem** ve **Ayarlar ve Yedek** bölümleri bulunur.
- Pinyin/Türkçe varsayılan görünürlüğü, otomatik oynatma, konuşma hızı ve günlük tekrar hedefi telefonda saklanır.
- Kullanıcı favorilerini, sınav sonuçlarını, sahne ilerlemesini, placement sonucunu ve ayarlarını tek `.json` dosyasına yedekleyebilir ve geri yükleyebilir.
- Yedekleme Android'in sistem dosya seçicisini kullanır; genel depolama izni gerekmez.
