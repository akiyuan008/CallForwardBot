package com.autoanswer.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

object WeComSender {
    private const val TAG = "WeComSender"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 完整发送链路：获取 token → 上传素材 → 发消息；失败重试 3 次指数退避 */
    fun sendRecording(c: Context, number: String, file: File, onDone: ((Boolean, String) -> Unit)? = null) {
        scope.launch {
            var ok = false
            var detail = ""
            for (attempt in 1..3) {
                try {
                    val (mediaId, isVoice) = WeComApi.uploadMedia(c, file)
                    val r = WeComApi.sendVoiceOrFile(c, mediaId, isVoice)
                    ok = true
                    detail = r.second
                    break
                } catch (e: Exception) {
                    detail = e.message ?: e.toString()
                    Log.e(TAG, "attempt $attempt failed: $detail")
                    if (attempt < 3) delay((2.0.pow(attempt) * 1000).toLong())
                }
            }
            Prefs.addHistory(c, number, file.absolutePath, file.length(), ok, detail)
            Log.i(TAG, "send finished ok=$ok detail=$detail")
            onDone?.invoke(ok, detail)
        }
    }

    fun sendTextTest(c: Context, onDone: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
                val r = WeComApi.sendText(c, "【来电转发助手】测试消息，时间：$time")
                onDone(true, r.second)
            } catch (e: Exception) {
                onDone(false, e.message ?: e.toString())
            }
        }
    }
}
