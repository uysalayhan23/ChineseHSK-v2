package com.ayhan.chineselearning

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import java.io.File
import java.util.concurrent.Executors

/** Kokoro runs separately so a native model failure cannot close the app UI. */
class KokoroSynthesisService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var tts: OfflineTts? = null

    private val messenger = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (message.what != MSG_SYNTHESIZE) return@Handler true
        val data = message.data ?: Bundle()
        val replyTo = message.replyTo
        val requestId = data.getInt(KEY_REQUEST_ID, -1)
        val text = data.getString(KEY_TEXT).orEmpty()
        val sid = data.getInt(KEY_SID, -1)
        val speed = data.getFloat(KEY_SPEED, 1.0f)
        val outputPath = data.getString(KEY_OUTPUT).orEmpty()
        if (replyTo == null || requestId < 0 || text.isBlank() || sid < 0 || outputPath.isBlank()) {
            sendReply(replyTo, requestId, false, outputPath)
            return@Handler true
        }

        executor.execute {
            val success = runCatching {
                val output = File(outputPath).canonicalFile
                val allowed = File(filesDir, "mandarin_tts_cache").canonicalFile
                require(output.path.startsWith(allowed.path + File.separator)) { "Invalid output path" }
                require(hasMemoryForModel()) { "Not enough memory for Kokoro" }
                val audio = createTts().generate(text, sid, speed)
                output.parentFile?.mkdirs()
                audio.save(output.absolutePath)
                require(output.isFile && output.length() > 1024L) { "Kokoro returned empty audio" }
            }.isSuccess
            sendReply(replyTo, requestId, success, outputPath)
        }
        true
    })

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        executor.shutdownNow()
        tts?.release()
        tts = null
        super.onDestroy()
    }

    private fun hasMemoryForModel(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        if (manager.isLowRamDevice) return false
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        return !memory.lowMemory && memory.availMem >= 3072L * 1024L * 1024L
    }

    private fun createTts(): OfflineTts {
        tts?.let { return it }
        val root = "mandarin_tts"
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = "${root}/model.onnx",
                    voices = "${root}/voices.bin",
                    tokens = "${root}/tokens.txt",
                    dataDir = "${root}/espeak-ng-data",
                    lexicon = "${root}/lexicon-us-en.txt,${root}/lexicon-zh.txt"
                ),
                numThreads = 2,
                provider = "cpu",
                debug = false
            ),
            ruleFsts = listOf("phone-zh.fst", "number-zh.fst", "date-zh.fst")
                .joinToString(",") { "${root}/$it" },
            maxNumSentences = 1
        )
        return OfflineTts(applicationContext.assets, config).also { tts = it }
    }

    private fun sendReply(replyTo: Messenger?, requestId: Int, success: Boolean, outputPath: String) {
        if (replyTo == null) return
        runCatching {
            val reply = Message.obtain(null, MSG_RESULT)
            reply.arg1 = if (success) 1 else 0
            reply.data = Bundle().apply {
                putInt(KEY_REQUEST_ID, requestId)
                putString(KEY_OUTPUT, outputPath)
            }
            replyTo.send(reply)
        }
    }

    companion object {
        const val MSG_SYNTHESIZE = 1
        const val MSG_RESULT = 2
        const val KEY_REQUEST_ID = "request_id"
        const val KEY_TEXT = "text"
        const val KEY_SID = "speaker_id"
        const val KEY_SPEED = "speed"
        const val KEY_OUTPUT = "output_path"
    }
}
