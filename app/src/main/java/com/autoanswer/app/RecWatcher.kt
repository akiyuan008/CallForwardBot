package com.autoanswer.app

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File

/**
 * 录音监听：不自己录音，轮询系统通话录音目录，发现新落盘文件且大小稳定后触发上传。
 * 兜底：挂断事件后 10s / 30s 额外各扫一次。
 */
class RecWatcher private constructor(context: Context) {

    companion object {
        private const val TAG = "RecWatcher"
        private const val POLL_MS = 5000L
        private const val MAX_AGE_MS = 180_000L
        private val EXT = listOf(".amr", ".m4a", ".mp3", ".wav", ".3gpp", ".aac")

        @Volatile
        var instance: RecWatcher? = null
        private var started = false

        fun start(c: Context) {
            if (started) return
            started = true
            instance = RecWatcher(c.applicationContext)
        }

        fun stop() {
            started = false
            instance?.handler?.removeCallbacksAndMessages(null)
            instance = null
        }

        fun candidateDirs(): List<File> {
            val root = Environment.getExternalStorageDirectory()
            return listOf(
                File(root, "MIUI/sound_recorder/call_rec"),
                File(root, "Music/CallRecord"),
                File(root, "Recordings/Call"),
                File(root, "Sounds/Call recordings"),
                File(root, "Record/Call")
            ).filter { it.isDirectory }
        }
    }

    private val ctx = context.applicationContext
    val handler = Handler(Looper.getMainLooper())
    private val knownFiles = HashSet<String>()
    private val pendingSizes = HashMap<String, Long>()
    private var lastCallNumber = ""
    private var lastCallEndAt = 0L

    private val pollTask = object : Runnable {
        override fun run() {
            scan()
            handler.postDelayed(this, POLL_MS)
        }
    }

    init {
        handler.post {
            scan(initializeOnly = true)
            handler.postDelayed(pollTask, POLL_MS)
        }
    }

    fun onCallEnded(number: String, endAt: Long) {
        lastCallNumber = number
        lastCallEndAt = endAt
        handler.postDelayed({ scan() }, 10_000)
        handler.postDelayed({ scan() }, 30_000)
    }

    /** 自检用：强制扫描并上传最新录音 */
    fun forceScanAndSend() {
        lastCallEndAt = System.currentTimeMillis()
        lastCallNumber = "selfcheck"
        scan()
    }

    private fun scan(initializeOnly: Boolean = false): Int {
        var found = 0
        val now = System.currentTimeMillis()
        for (d in candidateDirs()) {
            val files = d.listFiles() ?: continue
            for (f in files) {
                if (!f.isFile) continue
                val name = f.name.lowercase()
                if (EXT.none { name.endsWith(it) }) continue
                val path = f.absolutePath
                if (initializeOnly) { knownFiles.add(path); continue }
                if (path in knownFiles) continue
                if (now - f.lastModified() > MAX_AGE_MS) { knownFiles.add(path); continue }
                val size = f.length()
                val prev = pendingSizes[path]
                if (prev != null && prev == size && size > 0) {
                    knownFiles.add(path)
                    pendingSizes.remove(path)
                    found++
                    handleNewFile(f)
                } else {
                    pendingSizes[path] = size
                }
            }
        }
        if (found > 0) Log.i(TAG, "found $found new recording(s)")
        return found
    }

    private fun handleNewFile(f: File) {
        val recent = System.currentTimeMillis() - lastCallEndAt in 0..120_000
        if (!recent) {
            Log.i(TAG, "new file but no recent call, skip: ${f.name}")
            return
        }
        Log.i(TAG, "upload ${f.name} (${f.length()} bytes) for call from $lastCallNumber")
        WeComSender.sendRecording(ctx, lastCallNumber, f)
    }
}
