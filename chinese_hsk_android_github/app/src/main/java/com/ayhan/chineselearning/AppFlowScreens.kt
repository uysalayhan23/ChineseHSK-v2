package com.ayhan.chineselearning

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FlowSuccess = Color(0xFF18794E)

@Composable
fun WelcomeScreen(onStartZero: () -> Unit, onPlacement: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("中文生活", color = MaterialTheme.colorScheme.secondary, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text("Çince Yolculuğu", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "HSK1'den HSK6'ya · 300 sahne · tamamen offline öğrenme",
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(38.dp))
            Button(
                onClick = onStartZero,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Sıfırdan Başla", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onPlacement, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Text("Seviyemi Belirle", color = Color.White, fontSize = 18.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "İlerleme, favoriler ve sınav sonuçları yalnızca bu telefonda saklanır.",
                color = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PlacementTestScreen(
    repo: ContentRepository,
    onBack: () -> Unit,
    onComplete: (level: Int, score: Int, total: Int) -> Unit
) {
    val questions = remember { repo.loadPlacementItems() }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }
    val levelScores = remember { mutableStateMapOf<Int, Int>() }
    val context = LocalContext.current
    val tts = remember { MandarinTtsPlayer(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    fun recommendedLevel(): Int {
        var highestConsecutive = 0
        for (level in 1..6) {
            if ((levelScores[level] ?: 0) >= 10) highestConsecutive = level else break
        }
        return if (highestConsecutive == 0) 1 else (highestConsecutive + 1).coerceAtMost(6)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background)))) {
        Column(Modifier.fillMaxSize()) {
            Text("← Geri", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onBack() }.padding(20.dp), fontWeight = FontWeight.Bold)

            if (questions.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Seviye tespit soru bankası yüklenemedi.", color = Color.White, textAlign = TextAlign.Center)
                }
                return@Column
            }

            if (finished) {
                val level = recommendedLevel()
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Seviye Sonucu", color = MaterialTheme.colorScheme.primary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text("HSK$level", color = MaterialTheme.colorScheme.secondary, fontSize = 48.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 12.dp))
                            Text("$score / ${questions.size} doğru", color = Color.DarkGray)
                            Text(
                                "Her seviyede 14 özgün soru · bir seviyeyi geçme eşiği 10/14",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 5.dp, bottom = 12.dp)
                            )
                            (1..6).forEach { hsk ->
                                val value = levelScores[hsk] ?: 0
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text("HSK$hsk", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    Text("$value / 14", color = if (value >= 10) FlowSuccess else Color.DarkGray)
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = { onComplete(level, score, questions.size) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Bu Seviyeden Başla")
                            }
                        }
                    }
                }
            } else {
                if (index > questions.lastIndex) index = questions.lastIndex
                val q = questions[index]
                val typeLabel = when (q.type) {
                    "listening" -> "Dinleme"
                    "grammar" -> "Dilbilgisi"
                    "vocabulary" -> "Sözcük"
                    else -> "Okuma"
                }
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("Seviye Tespit Sınavı", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${index + 1} / ${questions.size} · HSK${q.level} · $typeLabel",
                        color = Color.White.copy(alpha = 0.76f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LinearProgressIndicator(
                        progress = { (index + 1).toFloat() / questions.size },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text(q.prompt, color = MaterialTheme.colorScheme.primary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            if (q.audioZh.isNotBlank()) {
                                Button(
                                    onClick = { tts.speak(q.audioZh, "PLACEMENT", 0.88f) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("🔊 Mandarin cümleyi dinle", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Text("Dinleme sorularında Çince cümle ekranda gösterilmez.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            q.options.forEachIndexed { optionIndex, option ->
                                val selected = answered == optionIndex
                                val correct = answered != null && optionIndex == q.correct
                                OutlinedButton(
                                    onClick = {
                                        if (answered == null) {
                                            answered = optionIndex
                                            if (optionIndex == q.correct) {
                                                score++
                                                levelScores[q.level] = (levelScores[q.level] ?: 0) + 1
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = when {
                                            correct -> Color(0xFFE6F4EA)
                                            selected -> Color(0xFFFFE8E6)
                                            else -> Color.Transparent
                                        }
                                    )
                                ) { Text(option, color = MaterialTheme.colorScheme.primary) }
                            }
                            if (answered != null) {
                                Button(
                                    onClick = {
                                        if (index < questions.lastIndex) {
                                            index++
                                            answered = null
                                        } else {
                                            finished = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text(if (index < questions.lastIndex) "Sonraki" else "Sonucu Gör", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    levels: List<LevelInfo>,
    progress: ProgressStore,
    progressVersion: Int,
    adminMode: Boolean,
    onContinue: () -> Unit,
    onLevels: () -> Unit,
    onFavorites: () -> Unit,
    onDailyReview: () -> Unit,
    onFreeStudy: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit
) {
    val mastered = remember(progressVersion) { progress.masteredCount() }
    val startLevel = progress.placementStartLevel()
    val favoriteCount = progress.favoriteIds().size
    val streak = progress.currentStreak()
    val userName = remember(progressVersion) { progress.userName() }
    val greeting = userName.trim().takeIf { it.isNotEmpty() }?.let { "Merhaba, $it" } ?: "Merhaba"

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("中文生活", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(greeting, color = MaterialTheme.colorScheme.onPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Çince öğrenme yolculuğun · HSK$startLevel", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f))
                }
                if (adminMode) {
                    Surface(color = Color(0xFF1B7F4C), contentColor = Color.White, shape = RoundedCornerShape(14.dp)) {
                        Text("ADMIN", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Color(0xFF231F28)), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Kurs İlerlemesi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("$mastered / 300 sahne tamamlandı", color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                            }
                            Text("🔥 $streak", color = Color(0xFFD2691E), fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { mastered / 300f },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            color = FlowSuccess,
                            trackColor = Color(0xFFE8E1ED)
                        )
                        if (adminMode) {
                            Text(
                                "Admin modu açık: tüm sahneler bu oturum için erişilebilir.",
                                color = Color(0xFF176B43),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("▶ Kaldığım Yerden Devam Et", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Black)
                }
            }

            item {
                DashboardAction(
                    "📚",
                    "HSK Seviyeleri",
                    if (adminMode) "ADMIN · 300 sahnenin tamamı açık" else "HSK1–HSK6 · 300 sahne",
                    onLevels
                )
            }
            item { DashboardAction("🔁", "Günlük Tekrar", "Bugünkü kelime tekrarlarını çalış", onDailyReview) }
            item { DashboardAction("🧪", "Serbest Çalışma", "Kelime, cümle, anlama, diyalog ve telaffuzu adet seçerek çalış", onFreeStudy) }
            item { DashboardAction("★", "Favorilerim", "$favoriteCount favori kelime", onFavorites) }
            item { DashboardAction("📊", "İlerlemem", "Sınavlar, seri ve seviye durumun", onProgress) }

            item {
                Button(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.96f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("⚙  Ayarlar", fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            }

            item {
                Text(
                    "Seviye Durumu",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(levels) { level ->
                val levelNo = level.id.removePrefix("HSK").toIntOrNull() ?: 1
                val count = remember(level.id, progressVersion) { progress.masteredCount(levelNo) }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f), contentColor = Color(0xFF231F28))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(level.title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (adminMode) "🔓 $count / 50" else "$count / 50",
                            color = if (adminMode || count == 50) FlowSuccess else Color.Gray
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun SettingsHubScreen(
    progress: ProgressStore,
    progressVersion: Int,
    adminMode: Boolean,
    onAdminModeChanged: (Boolean) -> Unit,
    onThemeChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onAdvancedSettings: () -> Unit,
    onSystemCheck: () -> Unit,
    onPlacement: () -> Unit
) {
    var savedName by remember(progressVersion) { mutableStateOf(progress.userName()) }
    var nameDraft by remember(savedName) { mutableStateOf(savedName) }
    var themeMode by remember(progressVersion) { mutableIntStateOf(progress.themeMode()) }
    var pinyinMode by remember(progressVersion) { mutableIntStateOf(progress.pinyinDisplayMode()) }
    var turkishMode by remember(progressVersion) { mutableIntStateOf(progress.turkishDisplayMode()) }
    var autoplay by remember(progressVersion) { mutableStateOf(progress.autoPlayDefault()) }
    var speed by remember(progressVersion) { mutableFloatStateOf(progress.playbackSpeed(1.0f)) }
    var dailyTarget by remember(progressVersion) { mutableIntStateOf(progress.dailyReviewTarget()) }
    var adminPassword by remember { mutableStateOf("") }
    var status by remember(adminMode) { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Header("Ayarlar", "Tüm uygulama tercihleri tek yerde", onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionCard("👤 Kullanıcı Profili") {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(40); status = "" },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Kullanıcı adı") }
                    )
                    Button(
                        onClick = {
                            val clean = nameDraft.trim()
                            progress.saveUserName(clean)
                            savedName = clean
                            status = "Kullanıcı adı kaydedildi"
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("Kaydet") }
                }
            }

            item {
                SettingsSectionCard("🎨 Tema") {
                    Text(
                        "Tema rengini seç. Pastel temalarda metin ve buton kontrastı otomatik korunur.",
                        color = Color(0xFF5F5963),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val themeChoices = listOf(
                        ProgressStore.THEME_PURPLE to "Mor",
                        ProgressStore.THEME_BLUE to "Mavi",
                        ProgressStore.THEME_GREEN to "Yeşil",
                        ProgressStore.THEME_ORANGE to "Turuncu",
                        ProgressStore.THEME_PINK to "Pembe",
                        ProgressStore.THEME_DARK to "Koyu",
                        ProgressStore.THEME_LIGHT to "Açık"
                    )
                    themeChoices.chunked(4).forEach { rowChoices ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowChoices.forEach { (mode, label) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = {
                                        themeMode = mode
                                        progress.saveThemeMode(mode)
                                        onThemeChanged(mode)
                                    },
                                    label = {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = if (themeMode == mode) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionCard("字幕 Pinyin ve Türkçe Çeviri") {
                    Text("Pinyin varsayılanı", fontWeight = FontWeight.Bold)
                    SettingsDisplayModeRow(pinyinMode) {
                        pinyinMode = it
                        progress.savePinyinDisplayMode(it)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Türkçe çeviri varsayılanı", fontWeight = FontWeight.Bold)
                    SettingsDisplayModeRow(turkishMode) {
                        turkishMode = it
                        progress.saveTurkishDisplayMode(it)
                    }
                }
            }

            item {
                SettingsSectionCard("🔊 Oynatma ve Ses") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Otomatik oynat", fontWeight = FontWeight.Bold)
                            Text("Sahne açıldığında replikler otomatik ilerlesin.", color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoplay,
                            onCheckedChange = {
                                autoplay = it
                                progress.saveAutoPlayDefault(it)
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Konuşma hızı: ${"%.2f".format(speed)}x", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.75f, 0.85f, 1.0f, 1.15f, 1.25f).forEach { value ->
                            FilterChip(
                                selected = kotlin.math.abs(speed - value) < 0.01f,
                                onClick = {
                                    speed = value
                                    progress.savePlaybackSpeed(value)
                                },
                                label = { Text("${value}x", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionCard("🔁 Günlük Tekrar Hedefi") {
                    Text("Günlük $dailyTarget kelime", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 12).forEach { value ->
                            FilterChip(
                                selected = dailyTarget == value,
                                onClick = {
                                    dailyTarget = value
                                    progress.saveDailyReviewTarget(value)
                                },
                                label = { Text("$value") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 20, 30).forEach { value ->
                            FilterChip(
                                selected = dailyTarget == value,
                                onClick = {
                                    dailyTarget = value
                                    progress.saveDailyReviewTarget(value)
                                },
                                label = { Text("$value") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(if (adminMode) "🔓 Admin Modu Aktif" else "🔐 Admin Girişi") {
                    if (adminMode) {
                        Text(
                            "Bu oturumda tüm HSK seviyeleri, 300 sahne ve sınav kilitleri açık. Normal kullanıcı ilerlemesi değiştirilmez.",
                            color = Color(0xFF176B43),
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = {
                                AdminSession.logout()
                                onAdminModeChanged(false)
                                status = "Admin modu kapatıldı"
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A263A))
                        ) { Text("Admin Modundan Çık") }
                    } else {
                        Text(
                            "Şifre doğruysa admin modu uygulama kapanana kadar açık kalır.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it; status = "" },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true,
                            label = { Text("Admin şifresi") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(
                            onClick = {
                                if (AdminSession.login(adminPassword)) {
                                    onAdminModeChanged(true)
                                    adminPassword = ""
                                    status = "Admin modu açıldı"
                                } else {
                                    status = "Şifre yanlış"
                                }
                            },
                            enabled = adminPassword.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("Admin Modunu Aç") }
                    }
                    if (status.isNotBlank()) {
                        Text(
                            status,
                            color = if (AdminSession.active || status.contains("kaydedildi")) FlowSuccess else Color(0xFFB3261E),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsNavigationCard(
                    icon = "🔔",
                    title = "Bildirimler, Yedekleme ve Gelişmiş Ayarlar",
                    subtitle = "Hatırlatıcı saatleri, bildirimler, JSON yedekleme ve veri seçenekleri",
                    onClick = onAdvancedSettings
                )
            }

            item {
                SettingsNavigationCard(
                    icon = "🩺",
                    title = "Sistem Kontrolü",
                    subtitle = "Offline Mandarin sesi, konuşma tanıma, cache ve içerik durumunu kontrol et",
                    onClick = onSystemCheck
                )
            }

            item {
                SettingsNavigationCard(
                    icon = "🎯",
                    title = "Seviye Testini Yenile",
                    subtitle = "Başlangıç HSK seviyeni yeniden belirle",
                    onClick = onPlacement
                )
            }

            item { Spacer(Modifier.height(22.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f), contentColor = Color(0xFF231F28)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsDisplayModeRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ProgressStore.DISPLAY_AUTO to "Otomatik",
            ProgressStore.DISPLAY_ON to "Açık",
            ProgressStore.DISPLAY_OFF to "Kapalı"
        ).forEach { (mode, label) ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(label, fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
private fun DashboardAction(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 27.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
            Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        }
    }
}

@Composable
fun DailyReviewScreen(repo: ContentRepository, progress: ProgressStore, onBack: () -> Unit) {
    val cards = remember {
        val all = repo.loadAllVocabularyCards()
        val favoriteIds = progress.favoriteIds()
        val weakIds = progress.weakWordIds()
        val target = progress.dailyReviewTarget()
        val weak = all.filter { weakIds.contains(it.card.id) }
            .sortedByDescending { progress.weakWordPriority(it.card.id) }
            .take(target)
        val favorites = all.filter { favoriteIds.contains(it.card.id) && !weakIds.contains(it.card.id) }.take(target)
        val lastScene = progress.lastSceneId()
        val recent = all.filter { it.sceneId == lastScene && !favoriteIds.contains(it.card.id) && !weakIds.contains(it.card.id) }.take(target)
        (weak + favorites + recent).distinctBy { it.card.id }.take(target).ifEmpty { all.take(target) }
    }
    LaunchedEffect(Unit) { progress.recordStudyActivity() }
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember(index) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background)))) {
        Column(Modifier.fillMaxSize()) {
            Text("← Geri", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onBack() }.padding(20.dp), fontWeight = FontWeight.Bold)
            Text("Günlük Tekrar", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text("Bugünkü hedef: ${progress.dailyReviewTarget()} kelime", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
            if (cards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tekrar kartı bulunamadı.", color = Color.White) }
            } else {
                if (index >= cards.size) index = cards.lastIndex
                val item = cards[index]
                Card(
                    Modifier.fillMaxWidth().weight(1f).padding(20.dp).clickable { flipped = !flipped },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.card.zh, color = MaterialTheme.colorScheme.primary, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                            if (flipped) {
                                Text(item.card.pinyin, color = Color.Gray, fontSize = 22.sp, modifier = Modifier.padding(top = 16.dp))
                                Text(item.card.tr, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                if (item.card.exampleZh.isNotBlank()) Text(item.card.exampleZh, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
                            } else {
                                Text("Anlamı görmek için karta dokun", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
                            }
                            Text("${item.levelId} · ${item.sceneTitleTr}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("← Önceki") }
                    Button(onClick = { if (index < cards.lastIndex) index++ }, enabled = index < cards.lastIndex, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Sonraki →", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
