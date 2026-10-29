package com.ayhan.chineselearning

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONObject

private val CheckOk = Color(0xFF18794E)
private val CheckWarn = Color(0xFFB26A00)

@Composable
fun SystemCheckScreen(
    repo: ContentRepository,
    progress: ProgressStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val audio = remember { DialogueAudioPlayer(context) }
    val recognizer = remember { OfflineMandarinRecognizer(context) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var offlineVoices by remember { mutableIntStateOf(0) }
    var nativeVoices by remember { mutableIntStateOf(0) }
    var cacheFiles by remember { mutableIntStateOf(0) }
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var voiceInstallError by remember { mutableStateOf(false) }

    val levels = remember { repo.loadLevels() }
    val totalScenes = levels.sumOf { it.sceneCount }
    val completeScenes = levels.sumOf { it.completeScenes }
    val totalDialogues = remember {
        runCatching {
            val text = context.assets.open("chinese_course/content_status.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(text).optJSONObject("audioProductionQueue")?.optInt("dialogue", 0) ?: 0
        }.getOrDefault(0)
    }

    val recognitionAvailable = remember(refreshTick) { recognizer.isAvailable() }
    val notificationGranted = remember(refreshTick) {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(refreshTick) {
        // Android TTS voice enumeration is asynchronous; give it a short moment to initialize.
        delay(900)
        audio.refreshNativeVoiceInventory()
        offlineVoices = audio.offlineVoiceCount()
        nativeVoices = audio.nativeMandarinVoiceCount()
        cacheFiles = audio.cachedTtsCount()
        cacheBytes = audio.cachedTtsBytes()
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.destroy()
            audio.shutdown()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) {
            TextButton(onClick = onBack) { Text("← Geri", color = MaterialTheme.colorScheme.secondary) }
            Text("Sistem Kontrolü", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "Offline çalışma için gerekli bileşenleri burada kontrol edebilirsin.",
                color = Color.White.copy(alpha = 0.74f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                StatusCard(
                    title = "Kurs İçeriği",
                    value = "$completeScenes / $totalScenes sahne · $totalDialogues diyalog",
                    ok = completeScenes == 300 && totalDialogues >= 30_000,
                    detail = "HSK1–HSK6 içerik paketi"
                )
            }
            item {
                StatusCard(
                    title = "Telefondaki Mandarin Sesleri",
                    value = if (nativeVoices > 0) "$nativeVoices ses bulundu · $offlineVoices çevrimdışı" else "Mandarin ses paketi henüz bulunamadı",
                    ok = nativeVoices > 0,
                    detail = "Uygulama karakterlere telefondaki Mandarin seslerini ayrı ayrı atar. Ses türü bilgisi sistemde varsa kadın, erkek veya çocuk sesi tercih edilir."
                )
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Mandarin Ses Paketi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Telefonun Android TTS ekranından yüksek kaliteli Mandarin seslerini indir. Kurulumdan sonra buraya dönüp Yeniden Kontrol Et'e bas.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 5.dp, bottom = 10.dp)
                        )
                        OutlinedButton(onClick = {
                            try {
                                context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                                voiceInstallError = false
                            } catch (_: Exception) {
                                voiceInstallError = true
                            }
                        }) { Text("Telefondaki ses paketlerini aç") }
                        if (voiceInstallError) {
                            Text("Bu telefonda ses paketi ekranı açılamadı. Android Ayarlar içindeki Metin okuma / TTS bölümünü kullan.", color = CheckWarn, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                StatusCard(
                    title = "Offline Telaffuz Tanıma",
                    value = if (recognitionAvailable) "Hazır · ${recognizer.engineLabel()}" else "Gömülü model bulunamadı",
                    ok = recognitionAvailable,
                    detail = if (recognitionAvailable)
                        "Mandarin tanıma uygulamaya gömülü Vosk modeliyle tamamen çevrimdışı çalışır."
                    else
                        "Bu APK offline Mandarin modelini içermiyor; APK yeniden oluşturulmalıdır."
                )
            }
            item {
                StatusCard(
                    title = "Bildirim İzni",
                    value = if (notificationGranted) "Hazır" else "İzin verilmedi",
                    ok = notificationGranted,
                    detail = "Yalnızca günlük çalışma hatırlatıcısı için kullanılır."
                )
            }
            item {
                StatusCard(
                    title = "Yerel Ses Cache'i",
                    value = "$cacheFiles dosya · ${humanBytes(cacheBytes)}",
                    ok = true,
                    detail = "Yaklaşık 350 MB sınırında otomatik yönetilir."
                )
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Uygulama Bilgisi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Sürüm: ${BuildConfig.VERSION_NAME}", color = Color.DarkGray, modifier = Modifier.padding(top = 6.dp))
                        Text("Android: ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}", color = Color.DarkGray)
                        Text("Başlangıç seviyesi: HSK${progress.placementStartLevel()}", color = Color.DarkGray)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { refreshTick++ },
                        modifier = Modifier.weight(1f)
                    ) { Text("Yeniden Kontrol Et") }
                    OutlinedButton(
                        onClick = {
                            audio.clearTtsCache()
                            refreshTick++
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Ses Cache'ini Temizle") }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String, ok: Boolean, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(if (ok) "✓" else "!", color = if (ok) CheckOk else CheckWarn, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(value, color = if (ok) CheckOk else CheckWarn, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                Text(detail, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

private fun humanBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
