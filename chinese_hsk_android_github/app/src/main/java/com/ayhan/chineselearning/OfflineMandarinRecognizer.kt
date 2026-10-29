package com.ayhan.chineselearning

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fully offline Mandarin recognizer backed by the embedded Vosk
 * vosk-model-small-cn-0.22 model.
 *
 * It never depends on Android/Google SpeechRecognizer or a network service.
 */
class OfflineMandarinRecognizer(context: Context) {
    companion object {
        const val MODEL_ASSET_DIR = "vosk-model-small-cn-0.22"
        const val MODEL_DISPLAY_NAME = "Vosk Mandarin small-cn 0.22"
        const val SAMPLE_RATE = 16_000f
        private const val MODEL_READY_MARKER = ".chinese_hsk_vosk_ready_v1"
        private const val MIC_TIMEOUT_MS = 8_000
    }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val modelLock = Any()

    @Volatile private var model: Model? = null
    @Volatile private var destroyed = false
    @Volatile private var runId = 0

    private var speechService: SpeechService? = null
    private var streamService: SpeechStreamService? = null
    private var activeRecognizer: Recognizer? = null

    fun isAvailable(): Boolean = runCatching {
        appContext.assets.open("$MODEL_ASSET_DIR/am/final.mdl").use { }
        appContext.assets.open("$MODEL_ASSET_DIR/conf/mfcc.conf").use { }
        true
    }.getOrDefault(false)

    fun engineLabel(): String = MODEL_DISPLAY_NAME

    fun start(
        source: ParcelFileDescriptor? = null,
        onListening: () -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val id = ++runId
        main.post { releaseActive() }

        io.execute {
            if (destroyed || id != runId) {
                runCatching { source?.close() }
                return@execute
            }

            val readyModel = try {
                ensureModel()
            } catch (t: Throwable) {
                runCatching { source?.close() }
                main.post {
                    if (id == runId && !destroyed) {
                        onError(
                            "Offline Mandarin modeli açılamadı: " +
                                (t.message ?: t.javaClass.simpleName)
                        )
                    }
                }
                return@execute
            }

            main.post {
                if (destroyed || id != runId) {
                    runCatching { source?.close() }
                    return@post
                }
                try {
                    if (source == null) {
                        startMicrophone(id, readyModel, onListening, onResult, onError)
                    } else {
                        startRecordedPcm(id, readyModel, source, onListening, onResult, onError)
                    }
                } catch (t: Throwable) {
                    runCatching { source?.close() }
                    releaseActive()
                    onError("Offline Mandarin tanıma başlatılamadı: " + (t.message ?: t.javaClass.simpleName))
                }
            }
        }
    }

    private fun startMicrophone(
        id: Int,
        readyModel: Model,
        onListening: () -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val recognizer = Recognizer(readyModel, SAMPLE_RATE)
        val service = SpeechService(recognizer, SAMPLE_RATE)
        activeRecognizer = recognizer
        speechService = service

        val delivered = AtomicBoolean(false)
        var bestText = ""

        fun finish(text: String?, error: String? = null) {
            if (!delivered.compareAndSet(false, true)) return
            val clean = text.orEmpty().trim()
            if (clean.isNotBlank()) onResult(clean)
            else onError(error ?: "Konuşma algılanamadı. Tekrar deneyin.")
            releaseActive()
        }

        val listener = object : RecognitionListener {
            override fun onPartialResult(hypothesis: String) {
                val partial = parseVoskText(hypothesis, "partial")
                if (partial.isNotBlank()) bestText = partial
            }

            override fun onResult(hypothesis: String) {
                if (id != runId || destroyed) return
                val text = parseVoskText(hypothesis, "text")
                if (text.isNotBlank()) {
                    bestText = text
                    finish(text)
                }
            }

            override fun onFinalResult(hypothesis: String) {
                if (id != runId || destroyed) return
                val text = parseVoskText(hypothesis, "text").ifBlank { bestText }
                finish(text)
            }

            override fun onError(exception: Exception) {
                if (id != runId || destroyed) return
                finish(bestText, "Offline Mandarin tanıma hatası: " + (exception.message ?: "bilinmeyen hata"))
            }

            override fun onTimeout() {
                if (id != runId || destroyed) return
                finish(bestText, "Ses algılanmadı. Mikrofonu yaklaştırıp tekrar deneyin.")
            }
        }

        if (!service.startListening(listener, MIC_TIMEOUT_MS)) {
            releaseActive()
            onError("Offline Mandarin tanıyıcı zaten çalışıyor.")
            return
        }
        onListening()
    }

    private fun startRecordedPcm(
        id: Int,
        readyModel: Model,
        source: ParcelFileDescriptor,
        onListening: () -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val recognizer = Recognizer(readyModel, SAMPLE_RATE)
        val input = ParcelFileDescriptor.AutoCloseInputStream(source)
        val service = SpeechStreamService(recognizer, input, SAMPLE_RATE)
        activeRecognizer = recognizer
        streamService = service

        val delivered = AtomicBoolean(false)
        var bestText = ""

        fun finish(text: String?, error: String? = null) {
            if (!delivered.compareAndSet(false, true)) return
            val clean = text.orEmpty().trim()
            if (clean.isNotBlank()) onResult(clean)
            else onError(error ?: "Konuşma algılanamadı. Tekrar deneyin.")
            releaseActive()
        }

        val listener = object : RecognitionListener {
            override fun onPartialResult(hypothesis: String) {
                val partial = parseVoskText(hypothesis, "partial")
                if (partial.isNotBlank()) bestText = partial
            }

            override fun onResult(hypothesis: String) {
                val text = parseVoskText(hypothesis, "text")
                if (text.isNotBlank()) bestText = text
            }

            override fun onFinalResult(hypothesis: String) {
                if (id != runId || destroyed) return
                val text = parseVoskText(hypothesis, "text").ifBlank { bestText }
                finish(text)
            }

            override fun onError(exception: Exception) {
                if (id != runId || destroyed) return
                finish(bestText, "Offline Mandarin ses analizi hatası: " + (exception.message ?: "bilinmeyen hata"))
            }

            override fun onTimeout() {
                if (id != runId || destroyed) return
                finish(bestText, "Ses kaydı analiz süresini aştı.")
            }
        }

        if (!service.start(listener)) {
            releaseActive()
            onError("Kayıt analizi başlatılamadı.")
            return
        }
        onListening()
    }

    private fun ensureModel(): Model {
        model?.let { return it }
        synchronized(modelLock) {
            model?.let { return it }

            if (!isAvailable()) {
                throw IOException(
                    "Gömülü $MODEL_ASSET_DIR bulunamadı. APK eksik modelle oluşturulmuş."
                )
            }

            val root = File(appContext.filesDir, "offline_asr")
            val modelDir = File(root, MODEL_ASSET_DIR)
            val marker = File(modelDir, MODEL_READY_MARKER)
            val expected = File(modelDir, "am/final.mdl")

            if (!marker.exists() || !expected.exists()) {
                if (modelDir.exists()) modelDir.deleteRecursively()
                modelDir.mkdirs()
                copyAssetTree(MODEL_ASSET_DIR, modelDir)
                if (!expected.exists()) {
                    modelDir.deleteRecursively()
                    throw IOException("Vosk Mandarin model dosyaları eksik.")
                }
                marker.writeText("vosk-model-small-cn-0.22\n")
            }

            return Model(modelDir.absolutePath).also { model = it }
        }
    }

    private fun copyAssetTree(assetPath: String, destination: File) {
        val children = appContext.assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            appContext.assets.open(assetPath).use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            return
        }

        destination.mkdirs()
        children.forEach { child ->
            copyAssetTree("$assetPath/$child", File(destination, child))
        }
    }

    private fun parseVoskText(json: String, key: String): String = runCatching {
        JSONObject(json).optString(key, "")
    }.getOrDefault("").replace(Regex("\\s+"), " ").trim()

    fun stop() {
        runId += 1
        main.post { releaseActive() }
    }

    fun destroy() {
        runId += 1
        destroyed = true
        main.post { releaseActive() }
        io.execute {
            synchronized(modelLock) {
                runCatching { model?.close() }
                model = null
            }
        }
        io.shutdown()
    }

    private fun releaseActive() {
        val mic = speechService
        speechService = null
        if (mic != null) {
            runCatching { mic.cancel() }
            runCatching { mic.shutdown() }
        }

        val stream = streamService
        streamService = null
        if (stream != null) {
            runCatching { stream.stop() }
        }

        val rec = activeRecognizer
        activeRecognizer = null
        runCatching { rec?.close() }
    }
}

fun chineseTextSimilarity(target: String, recognized: String): Int {
    fun normalize(s: String): String = s
        .replace(Regex("[\\s，。！？、,.!?;；:'\"“”‘’（）()\\-]"), "")
        .trim()

    val a = normalize(target)
    val b = normalize(recognized)
    if (a.isEmpty() && b.isEmpty()) return 100
    if (a.isEmpty() || b.isEmpty()) return 0

    val prev = IntArray(b.length + 1) { it }
    val cur = IntArray(b.length + 1)
    for (i in 1..a.length) {
        cur[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
        }
        for (j in prev.indices) prev[j] = cur[j]
    }
    val distance = prev[b.length]
    val maxLen = maxOf(a.length, b.length)
    return (((maxLen - distance).toFloat() / maxLen) * 100)
        .toInt()
        .coerceIn(0, 100)
}
