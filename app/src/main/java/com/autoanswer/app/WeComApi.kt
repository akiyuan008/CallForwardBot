package com.autoanswer.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

object WeComApi {

    private val client = OkHttpClient()
    private val JSON_MT = "application/json; charset=utf-8".toMediaType()
    private val BIN_MT = "application/octet-stream".toMediaType()

    /** 企业微信语音素材上限 2MB，超过则降级为文件消息（20MB） */
    const val VOICE_MAX_BYTES = 2L * 1024 * 1024

    class ApiException(msg: String) : Exception(msg)

    private fun checkErr(json: String) {
        val o = JSONObject(json)
        val code = o.optInt("errcode", -1)
        if (code != 0) throw ApiException("errcode=$code ${o.optString("errmsg")}")
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw ApiException("HTTP ${resp.code}: $body")
            body
        }
    }

    private suspend fun httpPost(url: String, body: RequestBody): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).post(body).build()).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw ApiException("HTTP ${resp.code}: $text")
            text
        }
    }

    suspend fun getToken(c: Context): String {
        val now = System.currentTimeMillis()
        val cached = Prefs.cachedToken(c)
        if (cached.isNotEmpty() && now < Prefs.tokenExpireAt(c)) return cached
        val corpId = Prefs.corpId(c)
        val secret = Prefs.corpSecret(c)
        if (corpId.isEmpty() || secret.isEmpty()) throw ApiException("未配置企业微信 corpid/secret")
        val url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=$corpId&corpsecret=$secret"
        val resp = httpGet(url)
        checkErr(resp)
        val o = JSONObject(resp)
        val token = o.getString("access_token")
        Prefs.saveToken(c, token, o.optInt("expires_in", 7200))
        return token
    }

    /** 上传录音为临时素材，返回 media_id；自动按大小选 voice / file 类型 */
    suspend fun uploadMedia(c: Context, file: File): Pair<String, Boolean> {
        val token = getToken(c)
        val isVoice = file.length() <= VOICE_MAX_BYTES
        val type = if (isVoice) "voice" else "file"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("media", file.name, file.asRequestBody(BIN_MT))
            .build()
        val url = "https://qyapi.weixin.qq.com/cgi-bin/media/upload?access_token=$token&type=$type"
        val resp = httpPost(url, body)
        checkErr(resp)
        return JSONObject(resp).getString("media_id") to isVoice
    }

    private suspend fun sendPayload(c: Context, payload: JSONObject): Pair<Boolean, String> {
        val token = getToken(c)
        val url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=$token"
        val resp = httpPost(url, payload.toString().toRequestBody(JSON_MT))
        checkErr(resp)
        return true to "msgid=${JSONObject(resp).optString("msgid")}"
    }

    suspend fun sendVoiceOrFile(c: Context, mediaId: String, isVoice: Boolean): Pair<Boolean, String> {
        val agentId = Prefs.agentId(c)
        val toUser = Prefs.toUser(c)
        if (agentId.isEmpty() || toUser.isEmpty()) throw ApiException("未配置 agentid / 家长 UserID")
        val msgType = if (isVoice) "voice" else "file"
        val payload = JSONObject()
        payload.put("touser", toUser)
        payload.put("msgtype", msgType)
        payload.put("agentid", agentId.toInt())
        val inner = JSONObject()
        inner.put("media_id", mediaId)
        payload.put(msgType, inner)
        payload.put("safe", 0)
        payload.put("enable_duplicate_check", 0)
        return sendPayload(c, payload)
    }

    suspend fun sendText(c: Context, content: String): Pair<Boolean, String> {
        val payload = JSONObject()
        payload.put("touser", Prefs.toUser(c))
        payload.put("msgtype", "text")
        payload.put("agentid", Prefs.agentId(c).toInt())
        val inner = JSONObject()
        inner.put("content", content)
        payload.put("text", inner)
        return sendPayload(c, payload)
    }
}
