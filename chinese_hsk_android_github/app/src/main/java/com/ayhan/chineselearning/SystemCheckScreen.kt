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
    var bundledVoices by remember { mutableIntStateOf(0) }
    var nativeVoiceOptions by remember { mutableStateOf(emptyList<NativeMandarinVoiceOption>()) }
    var selectedVoiceSlots by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
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
        bundledVoices = audio.bundledVoiceCount()
        nativeVoiceOptions = audio.nativeVoiceOptions()
        selectedVoiceSlots = NATIVE_VOICE_SLOTS.associate { (key, _) -> key to audio.selectedNativeVoiceName(key) }
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
                    title = "Uygulamadaki Mandarin Ses Paketi",
                    value = if (bundledVoices >= 4) "$bundledVoices ayrı ses · tamamen çevrimdışı" else "Ses paketi bulunamadı",
                    ok = bundledVoices >= 4,
                    detail = if (bundledVoices >= 4)
                        "174 konuşmacı seçeneği. Model 8 kHz ses üretir ve çoğunlukla nötr konuşur."
                    else
                        "Bu APK AISHELL-3 ses paketini içermiyor. Uygulamanın sesli sürümü gerekir."
                )
            }
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
                    detail = when {
                        nativeVoices == 1 -> "Telefonda tek sistem sesi var; uygulamanın kendi paketi 174 ayrı Mandarin konuşmacısı içerir."
                        nativeVoices in 2..3 -> "Uygulamanın kendi ses paketi telefondan bağımsız olarak farklı konuşmacılar sunar."
                        nativeVoices >= 4 -> "Uygulama sesleri yanında listelenir; istersen aşağıdan dinleyip değiştirebilirsin."
                        else -> "Uygulamanın kendi Mandarin sesleri telefona ek ses kurmadan çalışır."
                    }
                )
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("İsteğe bağlı telefon sesleri", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Telefonunun ek sistem seslerini yönetmek istersen Android ayarlarını açabilirsin. Uygulamanın kendi sesleri için buna gerek yok.",
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
                        }) { Text("Telefonun TTS ayarlarını aç") }
                        if (voiceInstallError) {
                            Text("Bu telefonda ses paketi ekranı açılamadı. Android Ayarlar içindeki Metin okuma / TTS bölümünü kullan.", color = CheckWarn, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                NativeVoiceSetupCard(
                    options = nativeVoiceOptions,
                    selections = selectedVoiceSlots,
                    onSelect = { archetype, voiceName ->
                        audio.assignNativeVoice(archetype, voiceName)
                        selectedVoiceSlots = selectedVoiceSlots + (archetype to voiceName)
                        audio.previewNativeVoice(voiceName, archetype)
                    },
                    onPreview = { archetype ->
                        audio.previewNativeVoice(selectedVoiceSlots[archetype], archetype)
                    }
                )
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

private val NATIVE_VOICE_SLOTS = listOf(
    "male" to "Erkek yetişkin",
    "female" to "Kadın yetişkin",
    "boy" to "Erkek genç",
    "girl" to "Kız çocuk"
)

@Composable
private fun NativeVoiceSetupCard(
    options: List<NativeMandarinVoiceOption>,
    selections: Map<String, String?>,
    onSelect: (String, String?) -> Unit,
    onPreview: (String) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Karakter seslerini eşleştir", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Otomatik seçim yüklü Mandarin seslerini farklı karakter türlerine dağıtır. Telefonda yalnızca bir Mandarin sesi varsa herkes aynı sesi kullanır; ek paket indir. İstersen sesleri dinleyip türlere kendin atayabilirsin.",
                color = Color.Gray,
                fontSize = 13.sp
            )
            if (options.isEmpty()) {
                Text("Önce yukarıdaki düğmeyle Mandarin ses paketi kur, sonra Yeniden Kontrol Et'e bas.", color = CheckWarn, fontSize = 13.sp)
            }
            NATIVE_VOICE_SLOTS.forEach { (archetype, title) ->
                var expanded by remember(archetype) { mutableStateOf(false) }
                val selectedName = selections[archetype]
                val selectedLabel = options.firstOrNull { it.name == selectedName }?.label
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                        Text(selectedLabel ?: "Otomatik seçim", color = Color.Gray, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { expanded = true }, enabled = options.isNotEmpty()) { Text("Seç") }
                    OutlinedButton(onClick = { onPreview(archetype) }, enabled = options.isNotEmpty()) { Text("Dinle") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Otomatik seçim") },
                            onClick = { expanded = false; onSelect(archetype, null) }
                        )
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.label} · ${if (option.offline) "çevrimdışı" else "internet"}") },
                                onClick = { expanded = false; onSelect(archetype, option.name) }
                            )
                        }
                    }
                }
            }
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
