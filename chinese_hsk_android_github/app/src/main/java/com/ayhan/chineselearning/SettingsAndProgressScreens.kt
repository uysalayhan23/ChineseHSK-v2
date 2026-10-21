package com.ayhan.chineselearning

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdvancedSettingsScreen(
    progress: ProgressStore,
    onBack: () -> Unit,
    onThemeChanged: (Int) -> Unit,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    var pinyinMode by remember { mutableIntStateOf(progress.pinyinDisplayMode()) }
    var turkishMode by remember { mutableIntStateOf(progress.turkishDisplayMode()) }
    var autoplay by remember { mutableStateOf(progress.autoPlayDefault()) }
    var speed by remember { mutableFloatStateOf(progress.playbackSpeed(1.0f)) }
    var dailyTarget by remember { mutableIntStateOf(progress.dailyReviewTarget()) }
    var themeMode by remember { mutableIntStateOf(progress.themeMode()) }
    var reminderEnabled by remember { mutableStateOf(progress.studyReminderEnabled()) }
    var reminderHour by remember { mutableIntStateOf(progress.studyReminderHour()) }
    var reminderMinute by remember { mutableIntStateOf(progress.studyReminderMinute()) }
    var status by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderEnabled = true
            progress.saveStudyReminder(true, reminderHour, reminderMinute)
            StudyReminderScheduler.schedule(context, reminderHour, reminderMinute)
            status = "Günlük çalışma hatırlatıcısı açıldı."
        } else {
            reminderEnabled = false
            progress.saveStudyReminder(false, reminderHour, reminderMinute)
            StudyReminderScheduler.cancel(context)
            status = "Bildirim izni verilmedi; hatırlatıcı kapalı kaldı."
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(progress.exportBackupJson())
                } ?: error("Dosya açılamadı.")
            }.onSuccess {
                status = "Yedek başarıyla dışa aktarıldı."
            }.onFailure {
                status = "Yedek oluşturulamadı: ${it.message ?: "bilinmeyen hata"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Dosya okunamadı.")
                progress.importBackupJson(json).getOrThrow()
            }.onSuccess { restored ->
                pinyinMode = progress.pinyinDisplayMode()
                turkishMode = progress.turkishDisplayMode()
                autoplay = progress.autoPlayDefault()
                speed = progress.playbackSpeed(1.0f)
                dailyTarget = progress.dailyReviewTarget()
                themeMode = progress.themeMode()
                reminderEnabled = progress.studyReminderEnabled()
                reminderHour = progress.studyReminderHour()
                reminderMinute = progress.studyReminderMinute()
                if (reminderEnabled) StudyReminderScheduler.schedule(context, reminderHour, reminderMinute)
                else StudyReminderScheduler.cancel(context)
                onThemeChanged(themeMode)
                onDataChanged()
                status = "$restored ayar/ilerleme kaydı geri yüklendi."
            }.onFailure {
                status = "Yedek geri yüklenemedi: ${it.message ?: "geçersiz dosya"}"
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background))
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("← Geri", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp))
                Text("Ayarlar ve Yedek", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp))
                Text("Uygulama tamamen offline çalışmaya devam eder.", color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp))
            }

            item {
                SettingsCard("Altyazı Varsayılanları") {
                    Text("Pinyin", fontWeight = FontWeight.Bold)
                    DisplayModeRow(pinyinMode) { mode ->
                        pinyinMode = mode
                        progress.savePinyinDisplayMode(mode)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Türkçe çeviri", fontWeight = FontWeight.Bold)
                    DisplayModeRow(turkishMode) { mode ->
                        turkishMode = mode
                        progress.saveTurkishDisplayMode(mode)
                    }
                    Text(
                        "Seviyeye göre seçeneğinde HSK1–2 daha fazla yardım, ileri seviyeler daha az yardım gösterir.",
                        color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item {
                SettingsCard("Ses ve Oynatma") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Sahneyi otomatik oynat", fontWeight = FontWeight.Bold)
                            Text("Yeni sahne açıldığında replikleri otomatik ilerlet.", color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoplay,
                            onCheckedChange = {
                                autoplay = it
                                progress.saveAutoPlayDefault(it)
                            }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Varsayılan konuşma hızı: ${"%.2f".format(speed)}x", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.75f, 0.85f, 1.0f, 1.15f, 1.25f).forEach { value ->
                            FilterChip(
                                selected = kotlin.math.abs(speed - value) < 0.01f,
                                onClick = {
                                    speed = value
                                    progress.savePlaybackSpeed(value)
                                },
                                label = { Text("${value}x") }
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard("Günlük Tekrar") {
                    Text("Günlük kelime hedefi", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 12, 15, 20, 30).forEach { value ->
                            FilterChip(
                                selected = dailyTarget == value,
                                onClick = {
                                    dailyTarget = value
                                    progress.saveDailyReviewTarget(value)
                                },
                                label = { Text("$value") }
                            )
                        }
                    }
                    Text("Favoriler ve son çalışılan sahnedeki kelimeler bu hedefe göre seçilir.", color = Color.Gray, fontSize = 12.sp)
                }
            }

            item {
                SettingsCard("Çalışma Hatırlatıcısı") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Günlük bildirim", fontWeight = FontWeight.Bold)
                            Text("Seçtiğin saatte Çince çalışmanı hatırlatır. İnternet gerekmez.", color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    reminderEnabled = false
                                    progress.saveStudyReminder(false, reminderHour, reminderMinute)
                                    StudyReminderScheduler.cancel(context)
                                } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    reminderEnabled = true
                                    progress.saveStudyReminder(true, reminderHour, reminderMinute)
                                    StudyReminderScheduler.schedule(context, reminderHour, reminderMinute)
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Saat: %02d:%02d".format(reminderHour, reminderMinute), fontWeight = FontWeight.Bold)
                    Text("Saat", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7, 8, 12, 18, 20, 21, 22).forEach { hour ->
                            FilterChip(
                                selected = reminderHour == hour,
                                onClick = {
                                    reminderHour = hour
                                    progress.saveStudyReminder(reminderEnabled, reminderHour, reminderMinute)
                                    if (reminderEnabled) StudyReminderScheduler.schedule(context, reminderHour, reminderMinute)
                                },
                                label = { Text("%02d".format(hour)) }
                            )
                        }
                    }
                    Text("Dakika", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 30).forEach { minute ->
                            FilterChip(
                                selected = reminderMinute == minute,
                                onClick = {
                                    reminderMinute = minute
                                    progress.saveStudyReminder(reminderEnabled, reminderHour, reminderMinute)
                                    if (reminderEnabled) StudyReminderScheduler.schedule(context, reminderHour, reminderMinute)
                                },
                                label = { Text(":%02d".format(minute)) }
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard("Görünüm") {
                    val themes = listOf(
                        ProgressStore.THEME_PURPLE to "Mor",
                        ProgressStore.THEME_DARK to "Koyu",
                        ProgressStore.THEME_LIGHT to "Açık"
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        themes.forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = {
                                    themeMode = mode
                                    progress.saveThemeMode(mode)
                                    onThemeChanged(mode)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard("Yerel Yedekleme") {
                    Text(
                        "Favoriler, sınav puanları, sahne ilerlemesi, placement sonucu ve ayarlar tek JSON dosyasına kaydedilir.",
                        color = Color.DarkGray, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { exportLauncher.launch("ChineseHSK_yedek.json") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Yedeği Dışa Aktar") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("Yedekten Geri Yükle") }
                    Text(
                        "Bulut hesabı gerekmez. Dosyanı istediğin güvenli yerde saklayabilirsin.",
                        color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                    if (status.isNotBlank()) {
                        Text(status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DisplayModeRow(selected: Int, onSelected: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ProgressStore.DISPLAY_AUTO to "Seviyeye göre",
            ProgressStore.DISPLAY_ON to "Her zaman açık",
            ProgressStore.DISPLAY_OFF to "Kapalı"
        ).forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelected(value) }, label = { Text(label) })
        }
    }
}

@Composable
fun ProgressOverviewScreen(progress: ProgressStore, onBack: () -> Unit) {
    val mastered = remember { progress.masteredCount() }
    val vocab = remember { progress.averageVocabularyExamScore() }
    val sentence = remember { progress.averageSentenceExamScore() }
    val placement = remember { progress.placementScore() }
    val favorites = remember { progress.favoriteIds().size }
    val weakWords = remember { progress.weakWordIds().size }
    val currentStreak = remember { progress.currentStreak() }
    val longestStreak = remember { progress.longestStreak() }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background))
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("← Geri", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp))
                Text("İlerlemem", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Genel Kurs", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("$mastered / 300 sahne tamamlandı", modifier = Modifier.padding(top = 6.dp))
                        LinearProgressIndicator(
                            progress = { mastered / 300f },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Favori kelime: $favorites · Zayıf kelime: $weakWords", color = Color.Gray, modifier = Modifier.padding(top = 10.dp))
                        Text("🔥 Seri: $currentStreak gün · En uzun: $longestStreak gün", color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreCard("Kelime", if (vocab.second == 0) "—" else "%${vocab.first}", "${vocab.second} sınav", Modifier.weight(1f))
                    ScoreCard("Cümle", if (sentence.second == 0) "—" else "%${sentence.first}", "${sentence.second} sınav", Modifier.weight(1f))
                }
            }
            if (placement.second > 0) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Son Seviye Testi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text("${placement.first} / ${placement.second} doğru", color = Color.Gray)
                            }
                            Text("HSK${progress.placementStartLevel()}", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Text("Seviye İlerlemesi", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(6) { index ->
                val level = index + 1
                val count = progress.masteredCount(level)
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("HSK$level", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("$count / 50", color = if (count == 50) Color(0xFF18794E) else Color.Gray)
                        }
                        LinearProgressIndicator(
                            progress = { count / 50f },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun ScoreCard(title: String, score: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.Gray, fontSize = 13.sp)
            Text(score, color = MaterialTheme.colorScheme.primary, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp))
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
