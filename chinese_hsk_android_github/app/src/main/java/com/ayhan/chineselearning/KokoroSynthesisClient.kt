package com.ayhan.chineselearning

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger

/** Main-process client for crash-isolated Kokoro inference. */
internal class KokoroSynthesisClient(context: Context) {
    private data class Request(val id: Int, val text: String, val sid: Int, val speed: Float, val output: File)

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ids = AtomicInteger(1)
    private val pending = ArrayDeque<Request>()
    private val callbacks = mutableMapOf<Int, (Boolean) -> Unit>()
    private var remote: Messenger? = null
    private var binding = false
    private var bound = false
    private var closed = false

    private val incoming = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (message.what != KokoroSynthesisService.MSG_RESULT) return@Handler true
        val id = message.data?.getInt(KokoroSynthesisService.KEY_REQUEST_ID, -1) ?: -1
        val callback = callbacks.remove(id)
        mainHandler.removeCallbacksAndMessages(id)
        callback?.invoke(message.arg1 == 1)
        true
    })

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: android.os.IBinder?) {
            remote = binder?.let(::Messenger)
            binding = false
            if (remote == null) failAll() else flushPending()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote = null
            binding = false
            bound = false
            failAll()
        }

        override fun onBindingDied(name: ComponentName?) {
            remote = null
            binding = false
            bound = false
            failAll()
            runCatching { appContext.unbindService(this) }
        }

        override fun onNullBinding(name: ComponentName?) {
            remote = null
            binding = false
            bound = false
            failAll()
            runCatching { appContext.unbindService(this) }
        }
    }

    fun synthesize(text: String, sid: Int, speed: Float, output: File, callback: (Boolean) -> Unit) {
        mainHandler.post {
            if (closed) {
                callback(false)
                return@post
            }
            val id = ids.getAndIncrement()
            pending.addLast(Request(id, text, sid, speed, output))
            callbacks[id] = callback
            mainHandler.postAtTime({
                val timedOut = callbacks.remove(id)
                if (timedOut != null) {
                    pending.removeIf { it.id == id }
                    timedOut(false)
                }
            }, id, android.os.SystemClock.uptimeMillis() + REQUEST_TIMEOUT_MS)
            if (remote != null) flushPending() else bindIfNeeded()
        }
    }

    private fun bindIfNeeded() {
        if (binding || bound || closed) return
        binding = true
        val intent = Intent(appContext, KokoroSynthesisService::class.java)
        val didBind = runCatching { appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        if (!didBind) {
            binding = false
            failAll()
        } else {
            bound = true
        }
    }

    private fun flushPending() {
        val messenger = remote ?: return
        while (pending.isNotEmpty()) {
            val request = pending.removeFirst()
            if (!callbacks.containsKey(request.id)) continue
            val message = Message.obtain(null, KokoroSynthesisService.MSG_SYNTHESIZE)
            message.data = Bundle().apply {
                putInt(KokoroSynthesisService.KEY_REQUEST_ID, request.id)
                putString(KokoroSynthesisService.KEY_TEXT, request.text)
                putInt(KokoroSynthesisService.KEY_SID, request.sid)
                putFloat(KokoroSynthesisService.KEY_SPEED, request.speed)
                putString(KokoroSynthesisService.KEY_OUTPUT, request.output.absolutePath)
            }
            message.replyTo = incoming
            try {
                messenger.send(message)
            } catch (_: RemoteException) {
                callbacks.remove(request.id)?.invoke(false)
            }
        }
    }

    private fun failAll() {
        pending.clear()
        val failed = callbacks.values.toList()
        callbacks.clear()
        failed.forEach { it(false) }
    }

    fun shutdown() {
        mainHandler.post {
            if (closed) return@post
            closed = true
            failAll()
            remote = null
            if (bound || binding) runCatching { appContext.unbindService(connection) }
            bound = false
            binding = false
        }
    }

    companion object {
        private const val REQUEST_TIMEOUT_MS = 90_000L
    }
}
