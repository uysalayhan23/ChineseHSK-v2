package com.ayhan.chineselearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChineseJourneyApp() }
    }
}

private val PurpleTop = Color(0xFF4B2B73)
private val PurpleBottom = Color(0xFF211132)
private val Accent = Color(0xFFFFC857)

private val speakerRoleTr = mapOf(
    "旁白" to "Anlatıcı",
    "张伟" to "Zhang Wei", "刘梅" to "Liu Mei", "张雨桐" to "Yutong", "张乐乐" to "Lele",
    "李晨" to "Li Chen", "咪咪" to "Mimi", "主持人" to "Sunucu", "乘客" to "Yolcu", "亲家" to "Dünür",
    "伴侣" to "Partner", "供应商" to "Tedarikçi", "保安" to "Güvenlik", "公司职员" to "Şirket çalışanı",
    "兽医" to "Veteriner", "创业者" to "Girişimci", "助理" to "Asistan", "医生" to "Doktor",
    "司机" to "Şoför", "同事" to "İş arkadaşı", "同事甲" to "İş arkadaşı A", "同事乙" to "İş arkadaşı B",
    "同学" to "Sınıf arkadaşı", "员工" to "Çalışan", "售货员" to "Satış görevlisi", "商户" to "Esnaf",
    "图书管理员" to "Kütüphaneci", "场地方经理" to "Mekân yöneticisi", "奶奶" to "Büyükanne",
    "好朋友" to "Yakın arkadaş", "孙辈" to "Torun", "客户" to "Müşteri", "家人" to "Aile üyesi",
    "宾客" to "Misafir", "导师" to "Danışman", "导游" to "Rehber", "小朋友" to "Çocuk",
    "居民" to "Mahalle sakini", "工作人员" to "Görevli", "年轻人" to "Genç", "年轻创业者" to "Genç girişimci",
    "年轻顾客" to "Genç müşteri", "店员" to "Mağaza görevlisi", "张雨桐伴侣" to "Zhang Yutong'un partneri",
    "律师" to "Avukat", "志愿者" to "Gönüllü", "快递员" to "Kurye", "技术志愿者" to "Teknik gönüllü",
    "护士" to "Hemşire", "招聘者" to "İşe alım görevlisi", "摊主" to "Tezgâhtar", "收银员" to "Kasiyer",
    "新同事" to "Yeni iş arkadaşı", "新同学" to "Yeni sınıf arkadaşı", "新员工" to "Yeni çalışan",
    "新朋友" to "Yeni arkadaş", "朋友" to "Arkadaş", "服务员" to "Servis görevlisi",
    "李晨妻子" to "Li Chen'in eşi", "爷爷" to "Büyükbaba", "王师傅" to "Usta Wang",
    "环保小组成员" to "Çevre grubu üyesi", "理发师" to "Kuaför", "理财顾问" to "Finans danışmanı",
    "社区代表" to "Toplum temsilcisi", "社区居民" to "Mahalle sakini", "社区工作人员" to "Toplum merkezi görevlisi",
    "社区负责人" to "Toplum merkezi sorumlusu", "经理" to "Müdür", "老师" to "Öğretmen",
    "老顾客" to "Eski müşteri", "表演者" to "Sanatçı", "记者" to "Gazeteci", "路人" to "Yoldan geçen",
    "邮局工作人员" to "Postane görevlisi", "邻居" to "Komşu", "酒店工作人员" to "Otel görevlisi",
    "银行工作人员" to "Banka görevlisi", "队友" to "Takım arkadaşı", "青年志愿者" to "Genç gönüllü",
    "面试官" to "Mülakatçı", "项目成员" to "Proje üyesi", "顾客" to "Müşteri"
)

private fun latinSpeakerName(pinyin: String): String {
    val normalized = Normalizer.normalize(pinyin, Normalizer.Form.NFD)
    return normalized.replace(Regex("\\p{M}+"), "").replace("ü", "u").replace("Ü", "U").trim()
}

private fun speakerDisplayName(
    zhName: String,
    showTurkish: Boolean,
    profiles: Map<String, CharacterProfile>
): String {
    val raw = zhName.ifBlank { "旁白" }
    if (!showTurkish) return raw
    speakerRoleTr[raw]?.let { return it }
    val profile = profiles[raw]
    val latin = profile?.pinyin.orEmpty().let(::latinSpeakerName)
    return latin.ifBlank { raw }
}

private enum class SceneMode { STORY, FLASHCARDS, COMPREHENSION, PRONUNCIATION, INTERACTIVE, SENTENCE_PRACTICE, EXAM_HUB, EXAM_VOCAB, EXAM_SENTENCE }

private enum class RootMode { WELCOME, DASHBOARD, LEVELS, PLACEMENT, DAILY_REVIEW, FREE_STUDY, WEAK_WORDS, HABITS, PROGRESS, SETTINGS, ADVANCED_SETTINGS, SYSTEM_CHECK }

@Composable
fun ChineseJourneyApp() {
    val context = LocalContext.current
    val repo = remember { ContentRepository(context) }
    val progress = remember { ProgressStore(context) }
    val levels = remember { repo.loadLevels() }
    var selectedLevel by remember { mutableStateOf<LevelInfo?>(null) }
    var selectedScene by remember { mutableStateOf<SceneInfo?>(null) }
    var sceneMode by remember { mutableStateOf(SceneMode.STORY) }
    var showFavorites by remember { mutableStateOf(false) }
    var progressVersion by remember { mutableIntStateOf(0) }
    var onboardingComplete by remember { mutableStateOf(progress.isOnboardingComplete()) }
    var themeMode by remember { mutableIntStateOf(progress.themeMode()) }
    var adminMode by remember { mutableStateOf(AdminSession.active) }
    var rootMode by remember {
        mutableStateOf(if (progress.isOnboardingComplete()) RootMode.DASHBOARD else RootMode.WELCOME)
    }
    LaunchedEffect(Unit) {
        if (progress.studyReminderEnabled()) {
            StudyReminderScheduler.schedule(context, progress.studyReminderHour(), progress.studyReminderMinute())
        }
    }

    fun openScene(scene: SceneInfo) {
        if (adminMode || (scene.isOpenable && progress.isSceneUnlocked(scene.level, scene.number))) {
            selectedScene = repo.loadScene(scene.level, scene.id)
            progress.saveLastScene(scene.id)
            sceneMode = SceneMode.STORY
        }
    }

    fun continueLearning() {
        val lastId = progress.lastSceneId()
        if (lastId.isNotBlank()) {
            val levelId = lastId.substringAfter("ZH_").substringBefore("_SC")
            val meta = runCatching { repo.loadSceneIndex(levelId).firstOrNull { it.id == lastId } }.getOrNull()
            if (meta != null && (adminMode || progress.isSceneUnlocked(meta.level, meta.number))) {
                openScene(meta)
                return
            }
        }
        val start = progress.placementStartLevel()
        for (levelNo in start..6) {
            val levelId = "HSK$levelNo"
            val candidate = repo.loadSceneIndex(levelId).firstOrNull {
                (adminMode || progress.isSceneUnlocked(levelId, it.number)) && !progress.isSceneMastered(it.id)
            }
            if (candidate != null) {
                openScene(candidate)
                return
            }
        }
        val finalScene = repo.loadSceneIndex("HSK6").lastOrNull()
        if (finalScene != null) openScene(finalScene)
    }

    val appColorScheme = when (themeMode) {
        ProgressStore.THEME_DARK -> darkColorScheme(
            primary = Color(0xFF8B5FBF),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF3A244B),
            onPrimaryContainer = Color(0xFFF8F4FF),
            secondary = Accent,
            onSecondary = Color(0xFF2B2100),
            background = Color(0xFF0F0C12),
            onBackground = Color(0xFFF8F4FF),
            surface = Color(0xFF1B171F),
            onSurface = Color(0xFFF8F4FF),
            surfaceVariant = Color(0xFF2A2430),
            onSurfaceVariant = Color(0xFFE7DFEA),
            outline = Color(0xFF9B8FA4)
        )
        ProgressStore.THEME_LIGHT -> lightColorScheme(
            primary = Color(0xFF5A2D82),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE9D9F5),
            onPrimaryContainer = Color(0xFF24152F),
            secondary = Color(0xFFD79A00),
            onSecondary = Color(0xFF2A1E00),
            background = Color(0xFFF8F5FA),
            onBackground = Color(0xFF201B24),
            surface = Color.White,
            onSurface = Color(0xFF201B24),
            surfaceVariant = Color(0xFFF0EAF4),
            onSurfaceVariant = Color(0xFF4D4552),
            outline = Color(0xFF7F7486)
        )
        ProgressStore.THEME_BLUE -> lightColorScheme(
            primary = Color(0xFF477A9E),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8EAF4),
            onPrimaryContainer = Color(0xFF173346),
            secondary = Color(0xFFC58B5A),
            onSecondary = Color(0xFF211408),
            background = Color(0xFFEEF6FA),
            onBackground = Color(0xFF18252D),
            surface = Color(0xFFFAFDFE),
            onSurface = Color(0xFF18252D),
            surfaceVariant = Color(0xFFE3EFF5),
            onSurfaceVariant = Color(0xFF3C5361),
            outline = Color(0xFF718894)
        )
        ProgressStore.THEME_GREEN -> lightColorScheme(
            primary = Color(0xFF4F7F68),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCEDE2),
            onPrimaryContainer = Color(0xFF183728),
            secondary = Color(0xFFB27B54),
            onSecondary = Color(0xFF231409),
            background = Color(0xFFF0F7F2),
            onBackground = Color(0xFF1C2921),
            surface = Color(0xFFFBFDFB),
            onSurface = Color(0xFF1C2921),
            surfaceVariant = Color(0xFFE5F0E8),
            onSurfaceVariant = Color(0xFF405448),
            outline = Color(0xFF75887B)
        )
        ProgressStore.THEME_ORANGE -> lightColorScheme(
            primary = Color(0xFFA86335),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF7DEC5),
            onPrimaryContainer = Color(0xFF46240F),
            secondary = Color(0xFF6F7E4B),
            onSecondary = Color.White,
            background = Color(0xFFFFF5EA),
            onBackground = Color(0xFF302219),
            surface = Color(0xFFFFFCF8),
            onSurface = Color(0xFF302219),
            surfaceVariant = Color(0xFFF5E8DA),
            onSurfaceVariant = Color(0xFF604A3B),
            outline = Color(0xFF947B69)
        )
        ProgressStore.THEME_PINK -> lightColorScheme(
            primary = Color(0xFF9A5C70),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF3DCE4),
            onPrimaryContainer = Color(0xFF45212E),
            secondary = Color(0xFF6A7C9B),
            onSecondary = Color.White,
            background = Color(0xFFFFF2F5),
            onBackground = Color(0xFF2E2025),
            surface = Color(0xFFFFFBFC),
            onSurface = Color(0xFF2E2025),
            surfaceVariant = Color(0xFFF6E6EB),
            onSurfaceVariant = Color(0xFF5C4650),
            outline = Color(0xFF8D747E)
        )
        else -> darkColorScheme(
            primary = Color(0xFF5A2D82),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF3A1E52),
            onPrimaryContainer = Color(0xFFF9F3FF),
            secondary = Accent,
            onSecondary = Color(0xFF2B2100),
            background = Color(0xFF160D1F),
            onBackground = Color(0xFFF9F3FF),
            surface = Color(0xFF24162F),
            onSurface = Color(0xFFF9F3FF),
            surfaceVariant = Color(0xFF352343),
            onSurfaceVariant = Color(0xFFE8DEED),
            outline = Color(0xFFA18EA9)
        )
    }

    BackHandler(enabled = true) {
        when {
            selectedScene != null && sceneMode != SceneMode.STORY -> sceneMode = SceneMode.STORY
            selectedScene != null -> {
                selectedScene = null
                sceneMode = SceneMode.STORY
            }
            showFavorites -> showFavorites = false
            selectedLevel != null -> selectedLevel = null
            rootMode == RootMode.PLACEMENT && !onboardingComplete -> rootMode = RootMode.WELCOME
            rootMode == RootMode.PLACEMENT && onboardingComplete -> rootMode = RootMode.SETTINGS
            rootMode == RootMode.ADVANCED_SETTINGS || rootMode == RootMode.SYSTEM_CHECK -> rootMode = RootMode.SETTINGS
            rootMode != RootMode.DASHBOARD && onboardingComplete -> rootMode = RootMode.DASHBOARD
            else -> Unit
        }
    }

    MaterialTheme(colorScheme = appColorScheme) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        val fullScreenStory = selectedScene != null && sceneMode == SceneMode.STORY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background)))
                .then(if (fullScreenStory) Modifier else Modifier.safeDrawingPadding())
        ) {
            when {
                !onboardingComplete && rootMode != RootMode.PLACEMENT -> WelcomeScreen(
                    onStartZero = {
                        progress.completeOnboarding(1)
                        onboardingComplete = true
                        rootMode = RootMode.DASHBOARD
                        progressVersion++
                    },
                    onPlacement = { rootMode = RootMode.PLACEMENT }
                )
                rootMode == RootMode.PLACEMENT && selectedScene == null && selectedLevel == null && !showFavorites -> PlacementTestScreen(
                    repo = repo,
                    onBack = { rootMode = if (onboardingComplete) RootMode.DASHBOARD else RootMode.WELCOME },
                    onComplete = { level, score, total ->
                        val returnToSettings = onboardingComplete
                        progress.savePlacementResult(level, score, total)
                        onboardingComplete = true
                        progressVersion++
                        rootMode = if (returnToSettings) RootMode.SETTINGS else RootMode.DASHBOARD
                    }
                )
                rootMode == RootMode.DAILY_REVIEW && selectedScene == null && selectedLevel == null && !showFavorites -> DailyReviewScreen(
                    repo = repo,
                    progress = progress,
                    onBack = { rootMode = RootMode.DASHBOARD }
                )
                rootMode == RootMode.FREE_STUDY && selectedScene == null && selectedLevel == null && !showFavorites -> FreeStudyScreen(
                    repo = repo,
                    progress = progress,
                    onBack = { rootMode = RootMode.DASHBOARD }
                )
                rootMode == RootMode.WEAK_WORDS && selectedScene == null && selectedLevel == null && !showFavorites -> WeakWordsScreen(
                    repo = repo,
                    progress = progress,
                    onBack = { rootMode = RootMode.DASHBOARD }
                )
                rootMode == RootMode.HABITS && selectedScene == null && selectedLevel == null && !showFavorites -> StudyHabitsScreen(
                    progress = progress,
                    onBack = { rootMode = RootMode.DASHBOARD }
                )
                rootMode == RootMode.PROGRESS && selectedScene == null && selectedLevel == null && !showFavorites -> ProgressOverviewScreen(
                    progress = progress,
                    onBack = { rootMode = RootMode.DASHBOARD }
                )
                rootMode == RootMode.SYSTEM_CHECK && selectedScene == null && selectedLevel == null && !showFavorites -> SystemCheckScreen(
                    repo = repo,
                    progress = progress,
                    onBack = { rootMode = RootMode.SETTINGS }
                )
                rootMode == RootMode.ADVANCED_SETTINGS && selectedScene == null && selectedLevel == null && !showFavorites -> AdvancedSettingsScreen(
                    progress = progress,
                    onBack = { rootMode = RootMode.SETTINGS },
                    onThemeChanged = { mode ->
                        themeMode = mode
                        progressVersion++
                    },
                    onDataChanged = {
                        onboardingComplete = progress.isOnboardingComplete()
                        themeMode = progress.themeMode()
                        progressVersion++
                    }
                )
                rootMode == RootMode.SETTINGS && selectedScene == null && selectedLevel == null && !showFavorites -> SettingsHubScreen(
                    progress = progress,
                    progressVersion = progressVersion,
                    adminMode = adminMode,
                    onAdminModeChanged = { enabled ->
                        AdminSession.active = enabled
                        adminMode = enabled
                        progressVersion++
                    },
                    onThemeChanged = { mode ->
                        themeMode = mode
                        progressVersion++
                    },
                    onBack = { rootMode = RootMode.DASHBOARD },
                    onAdvancedSettings = { rootMode = RootMode.ADVANCED_SETTINGS },
                    onSystemCheck = { rootMode = RootMode.SYSTEM_CHECK },
                    onPlacement = { rootMode = RootMode.PLACEMENT }
                )
                showFavorites -> FavoritesScreen(repo, progress) { showFavorites = false }
                selectedScene != null && sceneMode == SceneMode.FLASHCARDS -> FlashCardScreen(
                    title = "Kelime Çalışması · ${selectedScene!!.titleTr}",
                    cards = repo.loadDialogueVocabulary(selectedScene!!),
                    progress = progress,
                    dialogueWords = repo.loadDialogueVocabulary(selectedScene!!),
                    onBack = { sceneMode = SceneMode.STORY }
                )
                selectedScene != null && sceneMode == SceneMode.COMPREHENSION -> ComprehensionScreen(
                    scene = selectedScene!!,
                    dialogueWords = repo.loadDialogueVocabulary(selectedScene!!),
                    onBack = { sceneMode = SceneMode.STORY }
                )
                selectedScene != null && sceneMode == SceneMode.PRONUNCIATION -> PronunciationPracticeScreen(
                    scene = selectedScene!!,
                    progress = progress,
                    onBack = { sceneMode = SceneMode.STORY }
                )
                selectedScene != null && sceneMode == SceneMode.INTERACTIVE -> InteractiveDialogueScreen(
                    scene = selectedScene!!,
                    dialogueWords = repo.loadDialogueVocabulary(selectedScene!!),
                    onBack = { sceneMode = SceneMode.STORY }
                )
                selectedScene != null && sceneMode == SceneMode.SENTENCE_PRACTICE -> SentencePracticeScreen(
                    scene = selectedScene!!,
                    dialogueWords = repo.loadDialogueVocabulary(selectedScene!!),
                    onBack = { sceneMode = SceneMode.STORY }
                )
                selectedScene != null && sceneMode == SceneMode.EXAM_HUB -> ExamHubScreen(
                    scene = selectedScene!!,
                    progress = progress,
                    onBack = { sceneMode = SceneMode.STORY },
                    onVocabularyExam = { sceneMode = SceneMode.EXAM_VOCAB },
                    onSentenceExam = { sceneMode = SceneMode.EXAM_SENTENCE }
                )
                selectedScene != null && sceneMode == SceneMode.EXAM_VOCAB -> VocabularyExamScreen(
                    scene = selectedScene!!,
                    progress = progress,
                    onBack = { sceneMode = SceneMode.EXAM_HUB },
                    onProgressChanged = { progressVersion++ }
                )
                selectedScene != null && sceneMode == SceneMode.EXAM_SENTENCE -> SentenceExamScreen(
                    scene = selectedScene!!,
                    progress = progress,
                    onBack = { sceneMode = SceneMode.EXAM_HUB },
                    onProgressChanged = { progressVersion++ }
                )
                selectedScene != null -> SceneScreen(
                    scene = selectedScene!!,
                    progress = progress,
                    progressVersion = progressVersion,
                    onBack = {
                        selectedScene = null
                        sceneMode = SceneMode.STORY
                    },
                    onFlashCards = { sceneMode = SceneMode.FLASHCARDS },
                    onComprehension = { sceneMode = SceneMode.COMPREHENSION },
                    onPronunciation = { sceneMode = SceneMode.PRONUNCIATION },
                    onInteractive = { sceneMode = SceneMode.INTERACTIVE },
                    onSentencePractice = { sceneMode = SceneMode.SENTENCE_PRACTICE },
                    onExam = { sceneMode = SceneMode.EXAM_HUB }
                )
                selectedLevel != null -> SceneListScreen(
                    level = selectedLevel!!,
                    scenes = remember(selectedLevel!!.id) { repo.loadSceneIndex(selectedLevel!!.id) },
                    progress = progress,
                    progressVersion = progressVersion,
                    adminMode = adminMode,
                    onBack = { selectedLevel = null },
                    onScene = { item -> openScene(item) }
                )
                rootMode == RootMode.LEVELS -> LevelScreen(
                    levels = levels,
                    onLevel = { selectedLevel = it },
                    onFavorites = { showFavorites = true },
                    onBack = { rootMode = RootMode.DASHBOARD }
                )
                else -> DashboardScreen(
                    levels = levels,
                    progress = progress,
                    progressVersion = progressVersion,
                    adminMode = adminMode,
                    onContinue = { continueLearning() },
                    onLevels = { rootMode = RootMode.LEVELS },
                    onFavorites = { showFavorites = true },
                    onDailyReview = { rootMode = RootMode.DAILY_REVIEW },
                    onFreeStudy = { rootMode = RootMode.FREE_STUDY },
                    onProgress = { rootMode = RootMode.PROGRESS },
                    onSettings = { rootMode = RootMode.SETTINGS }
                )
            }
        }
        }
    }
}

@Composable
fun Header(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        if (onBack != null) {
            Text(
                text = "← Geri",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { onBack() }.padding(bottom = 12.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(title, color = MaterialTheme.colorScheme.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f), fontSize = 15.sp)
        }
    }
}

@Composable
private fun LevelScreen(levels: List<LevelInfo>, onLevel: (LevelInfo) -> Unit, onFavorites: () -> Unit, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize()) {
        Header("Çince Yolculuğu", "HSK1'den HSK6'ya · Basitleştirilmiş Çince · Offline", onBack)
        Button(
            onClick = onFavorites,
            modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("★ Favori Kelimelerimi Çalış", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(levels) { level ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onLevel(level) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(level.title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            Text("${level.completeScenes}/${level.sceneCount} tam", color = Color.DarkGray)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(level.storyArc, color = Color(0xFF4C4652))
                        if (level.blueprintReadyScenes > 0) {
                            Spacer(Modifier.height(5.dp))
                            Text("${level.blueprintReadyScenes}/${level.sceneCount} sahnenin planı hazır", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { if (level.sceneCount == 0) 0f else level.completeScenes.toFloat() / level.sceneCount },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0xFFE7DDF0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneListScreen(
    level: LevelInfo,
    scenes: List<SceneInfo>,
    progress: ProgressStore,
    progressVersion: Int,
    adminMode: Boolean,
    onBack: () -> Unit,
    onScene: (SceneInfo) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Header(level.title, if (adminMode) "🔓 Admin modu · Tüm sahneler açık" else "Sahneler sınav başarısına göre sırayla açılır", onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(scenes) { scene ->
                val unlockedNormally = remember(scene.id, progressVersion) { progress.isSceneUnlocked(scene.level, scene.number) }
                val unlocked = adminMode || unlockedNormally
                val mastered = remember(scene.id, progressVersion) { progress.isSceneMastered(scene.id) }
                val openable = adminMode || (scene.isOpenable && unlocked)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = openable) { onScene(scene) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (openable) Color.White else Color.White.copy(alpha = 0.58f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = if (unlocked) MaterialTheme.colorScheme.primary else Color.Gray) {
                            Text(
                                "%02d".format(scene.number),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(scene.titleZh, fontWeight = FontWeight.Bold, color = Color(0xFF271936))
                            Text(scene.titleTr, color = Color.DarkGray, fontSize = 13.sp)
                        }
                        Text(
                            when {
                                mastered -> "✓ Geçildi"
                                adminMode -> "🔓 Admin"
                                !unlocked -> "🔒 Kilitli"
                                scene.complete -> "Açık"
                                scene.productionStatus == "dialogue_draft" -> "Taslak"
                                scene.productionStatus == "blueprint_ready" -> "Plan hazır"
                                else -> "Planlı"
                            },
                            color = if (mastered) Color(0xFF18794E) else if (unlocked) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneScreen(
    scene: SceneInfo,
    progress: ProgressStore,
    progressVersion: Int,
    onBack: () -> Unit,
    onFlashCards: () -> Unit,
    onComprehension: () -> Unit,
    onPronunciation: () -> Unit,
    onInteractive: () -> Unit,
    onSentencePractice: () -> Unit,
    onExam: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { ContentRepository(context) }
    val audioPlayer = remember(scene.id) { DialogueAudioPlayer(context) }
    val soundscape = remember(scene.id) { SceneSoundscapePlayer(context) }
    val characterProfiles = remember { repo.loadCharacters().associateBy { it.nameZh } }
    val locationProfiles = remember { repo.loadLocations().associateBy { it.id } }
    val location = remember(scene.id) { locationProfiles[scene.production.locationId] }

    DisposableEffect(scene.id) {
        soundscape.start(scene)
        onDispose {
            audioPlayer.shutdown()
            soundscape.shutdown()
        }
    }

    var showPinyin by remember(scene.id) {
        mutableStateOf(
            when (progress.pinyinDisplayMode()) {
                ProgressStore.DISPLAY_ON -> true
                ProgressStore.DISPLAY_OFF -> false
                else -> scene.learning.defaultSubtitleMode.contains("PINYIN")
            }
        )
    }
    var showTurkish by remember(scene.id) {
        mutableStateOf(
            when (progress.turkishDisplayMode()) {
                ProgressStore.DISPLAY_ON -> true
                ProgressStore.DISPLAY_OFF -> false
                else -> scene.learning.defaultSubtitleMode.contains("TR")
            }
        )
    }
    var currentIndex by remember(scene.id) {
        mutableIntStateOf(
            progress.dialoguePosition(scene.id).coerceIn(0, (scene.dialogues.size - 1).coerceAtLeast(0))
        )
    }
    var autoPlay by remember(scene.id) { mutableStateOf(progress.autoPlayDefault()) }
    var speakingNow by remember(scene.id) { mutableStateOf(false) }
    var activeAudioDialogueId by remember(scene.id) { mutableStateOf<String?>(null) }
    var playbackGeneration by remember(scene.id) { mutableIntStateOf(0) }
    var speed by remember(scene.id) {
        mutableFloatStateOf(progress.playbackSpeed(scene.learning.defaultSpeechSpeed))
    }
    var narratorPlaying by remember(scene.id) { mutableStateOf(false) }
    var narratorAutoStarted by remember(scene.id) { mutableStateOf(false) }
    var showStudyMenu by remember(scene.id) { mutableStateOf(false) }
    var controlsVisible by remember(scene.id) { mutableStateOf(true) }

    val current = scene.dialogues.getOrNull(currentIndex)
    val mastered = remember(progressVersion, scene.id) { progress.isSceneMastered(scene.id) }

    BackHandler(enabled = showStudyMenu) {
        showStudyMenu = false
    }

    LaunchedEffect(currentIndex, scene.id) {
        progress.saveDialoguePosition(scene.id, currentIndex)
    }

    LaunchedEffect(currentIndex, autoPlay, speed, scene.id) {
        if (autoPlay && current != null) {
            playbackGeneration += 1
            val thisPlayback = playbackGeneration
            activeAudioDialogueId = current.id
            speakingNow = true
            audioPlayer.playDialogue(current, speed) {
                if (thisPlayback != playbackGeneration) return@playDialogue
                speakingNow = false
                activeAudioDialogueId = null
                playbackGeneration += 1
                if (currentIndex < scene.dialogues.lastIndex) {
                    currentIndex += 1
                } else {
                    autoPlay = false
                }
            }
        }
    }

    LaunchedEffect(scene.id) {
        val narrator = scene.production.narrator
        if (!narratorAutoStarted && currentIndex == 0 && narrator.zh.isNotBlank()) {
            narratorAutoStarted = true
            narratorPlaying = true
            audioPlayer.playNarrator(scene.id, narrator, 0.90f) {
                narratorPlaying = false
                autoPlay = true
            }
        }
    }

    if (scene.dialogues.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF100D13)).safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF251B2D),
                contentColor = Color.White,
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    "Bu sahnenin tam diyalogları henüz üretim aşamasında.",
                    modifier = Modifier.padding(24.dp),
                    color = Color.White
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SceneStage(
            scene = scene,
            location = location,
            characterProfiles = characterProfiles,
            activeSpeaker = current?.speaker.orEmpty(),
            activeDialogueId = activeAudioDialogueId,
            isSpeaking = speakingNow && activeAudioDialogueId == current?.id && !narratorPlaying,
            dialogueZh = current?.zh.orEmpty(),
            dialoguePinyin = current?.pinyin.orEmpty(),
            dialogueTr = current?.tr.orEmpty(),
            showPinyin = showPinyin,
            showTurkish = showTurkish,
            speakerLabel = speakerDisplayName(current?.speaker.orEmpty(), showTurkish, characterProfiles),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scene.id) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    scene.titleZh,
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    scene.titleTr,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            if (controlsVisible) {
                Button(
                    onClick = {
                        autoPlay = false
                        speakingNow = false
                        activeAudioDialogueId = null
                        playbackGeneration += 1
                        audioPlayer.stop()
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.TopStart),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF211D25),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("←", fontSize = 21.sp, fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = { showStudyMenu = true },
                    modifier = Modifier.align(Alignment.TopEnd),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF211D25),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (mastered) "✓ Çalış" else "Çalış", fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Surface(
                    color = Accent,
                    contentColor = Color(0xFF2B2100),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        speakerDisplayName(current?.speaker.orEmpty(), showTurkish, characterProfiles),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${currentIndex + 1} / ${scene.dialogues.size}",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (autoPlay) "OTOMATİK" else "MANUEL",
                    color = if (autoPlay) Accent else Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / scene.dialogues.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Accent,
                trackColor = Color.White.copy(alpha = 0.22f)
            )

            Spacer(Modifier.height(8.dp))

            current?.let { line ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.58f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            line.zh,
                            color = Color.White,
                            fontSize = 21.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 3
                        )
                        if (showPinyin && line.pinyin.isNotBlank()) {
                            Text(
                                line.pinyin,
                                color = Accent.copy(alpha = 0.96f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 3.dp),
                                maxLines = 2
                            )
                        }
                        if (showTurkish && line.tr.isNotBlank()) {
                            Text(
                                line.tr,
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 3.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (controlsVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPinyin = !showPinyin },
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showPinyin) Accent else Color(0xFF2B2730),
                            contentColor = if (showPinyin) Color(0xFF2B2100) else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (showPinyin) "Pinyin ✓" else "Pinyin", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { showTurkish = !showTurkish },
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showTurkish) Accent else Color(0xFF2B2730),
                            contentColor = if (showTurkish) Color(0xFF2B2100) else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (showTurkish) "Türkçe ✓" else "Türkçe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                }

            if (controlsVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            autoPlay = false
                            speakingNow = false
                            activeAudioDialogueId = null
                            playbackGeneration += 1
                            audioPlayer.stop()
                            if (currentIndex > 0) currentIndex--
                        },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f).height(54.dp),
                        contentPadding = PaddingValues(horizontal = 5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B2730),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1B181E),
                            disabledContentColor = Color(0xFF726A78)
                        ),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text("← Geri", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (autoPlay) {
                                autoPlay = false
                                speakingNow = false
                                activeAudioDialogueId = null
                                playbackGeneration += 1
                                audioPlayer.stop()
                            } else {
                                autoPlay = true
                            }
                        },
                        modifier = Modifier.weight(1.22f).height(58.dp),
                        contentPadding = PaddingValues(horizontal = 5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color(0xFF2B2100)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            if (autoPlay) "⏸ Duraklat" else "▶ Başlat",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            autoPlay = false
                            speakingNow = false
                            activeAudioDialogueId = null
                            playbackGeneration += 1
                            audioPlayer.stop()
                            if (currentIndex < scene.dialogues.lastIndex) currentIndex++
                        },
                        enabled = currentIndex < scene.dialogues.lastIndex,
                        modifier = Modifier.weight(1f).height(54.dp),
                        contentPadding = PaddingValues(horizontal = 5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B2730),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1B181E),
                            disabledContentColor = Color(0xFF726A78)
                        ),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text("İleri →", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

            }
        }
    }

    if (showStudyMenu) {
        ModalBottomSheet(
            onDismissRequest = { showStudyMenu = false },
            containerColor = Color(0xFF1B151F),
            contentColor = Color.White,
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 10.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(50))
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text("Sahne Çalışmaları", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Konuşma hızı: ${"%.2f".format(speed)}x",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 5.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.75f, 0.85f, 1.0f, 1.15f, 1.25f).forEach { value ->
                        Button(
                            onClick = {
                                speed = value
                                progress.savePlaybackSpeed(value)
                                if (autoPlay) {
                                    autoPlay = false
                                    audioPlayer.stop()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (kotlin.math.abs(speed - value) < 0.01f) Accent else Color(0xFF302937),
                                contentColor = if (kotlin.math.abs(speed - value) < 0.01f) Color(0xFF2B2100) else Color.White
                            )
                        ) {
                            Text("${value}x", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        showStudyMenu = false
                        onFlashCards()
                    },
                    enabled = scene.learning.vocabularyCards.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🃏 Kelime Flash Kartları (${scene.learning.vocabularyCards.size})") }

                OutlinedButton(
                    onClick = {
                        showStudyMenu = false
                        onComprehension()
                    },
                    enabled = scene.learning.comprehensionQuestions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧠 Anlama Çalışması", color = Color.White) }

                OutlinedButton(
                    onClick = {
                        showStudyMenu = false
                        onPronunciation()
                    },
                    enabled = scene.learning.pronunciationItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🎙 Telaffuz Çalışması", color = Color.White) }

                OutlinedButton(
                    onClick = {
                        showStudyMenu = false
                        onInteractive()
                    },
                    enabled = scene.learning.interactiveDialogue.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("💬 Seçimli Diyalog", color = Color.White) }

                OutlinedButton(
                    onClick = {
                        showStudyMenu = false
                        onSentencePractice()
                    },
                    enabled = scene.learning.sentenceExercises.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✍ Cümle Alıştırmaları", color = Color.White) }

                Button(
                    onClick = {
                        showStudyMenu = false
                        onExam()
                    },
                    enabled = scene.learning.vocabularyCards.isNotEmpty() && scene.learning.sentenceExercises.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFF2B2100)
                    )
                ) { Text("🎓 Sınavlar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
