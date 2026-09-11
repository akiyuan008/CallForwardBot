package com.autoanswer.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Prefs {
    private const val SP = "app_config"

    private fun sp(c: Context) = c.getSharedPreferences(SP, Context.MODE_PRIVATE)

    fun corpId(c: Context) = sp(c).getString("corp_id", "") ?: ""
    fun setCorpId(c: Context, v: String) = sp(c).edit().putString("corp_id", v.trim()).apply()

    fun corpSecret(c: Context): String {
        val raw = sp(c).getString("corp_secret", "") ?: ""
        return if (raw.isEmpty()) "" else Crypto.dec(raw)
    }

    fun setCorpSecret(c: Context, v: String) =
        sp(c).edit().putString("corp_secret", if (v.isEmpty()) "" else Crypto.enc(v.trim())).apply()

    fun agentId(c: Context) = sp(c).getString("agent_id", "") ?: ""
    fun setAgentId(c: Context, v: String) = sp(c).edit().putString("agent_id", v.trim()).apply()

    fun toUser(c: Context) = sp(c).getString("to_user", "") ?: ""
    fun setToUser(c: Context, v: String) = sp(c).edit().putString("to_user", v.trim()).apply()

    fun answerDelaySec(c: Context) = sp(c).getInt("answer_delay", 3)
    fun setAnswerDelaySec(c: Context, v: Int) = sp(c).edit().putInt("answer_delay", v.coerceIn(1, 30)).apply()

    fun whitelist(c: Context): Set<String> = sp(c).getStringSet("whitelist", emptySet()) ?: emptySet()

    fun setWhitelist(c: Context, set: Set<String>) = sp(c).edit().putStringSet("whitelist", set).apply()

    /** 支持精确匹配、前缀匹配（如 0755）和后缀匹配（如缺国家码） */
    fun isWhitelisted(c: Context, number: String?): Boolean {
        if (number.isNullOrEmpty()) return false
        val n = number.replace(" ", "").replace("-", "")
        return whitelist(c).any { w ->
            w.isNotEmpty() && (n == w || n.startsWith(w) || n.endsWith(w))
        }
    }

    // ---- token 缓存（企业微信 7200 秒，提前 60 秒过期） ----
    fun cachedToken(c: Context) = sp(c).getString("token", "") ?: ""
    fun tokenExpireAt(c: Context) = sp(c).getLong("token_expire", 0L)
    fun saveToken(c: Context, token: String, expireInSec: Int) =
        sp(c).edit()
            .putString("token", token)
            .putLong("token_expire", System.currentTimeMillis() + expireInSec * 1000L - 60_000L)
            .apply()

    // ---- 发送历史 ----
    fun history(c: Context): JSONArray {
        val raw = sp(c).getString("history", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    fun addHistory(c: Context, number: String, file: String, size: Long, ok: Boolean, detail: String) {
        val arr = history(c)
        val o = JSONObject()
        o.put("time", System.currentTimeMillis())
        o.put("number", number)
        o.put("file", file)
        o.put("size", size)
        o.put("ok", ok)
        o.put("detail", detail)
        arr.put(o)
        val trimmed = JSONArray()
        val start = maxOf(0, arr.length() - 200)
        for (i in start until arr.length()) trimmed.put(arr.getJSONObject(i))
        sp(c).edit().putString("history", trimmed.toString()).apply()
    }

    fun clearHistory(c: Context) = sp(c).edit().putString("history", "[]").apply()
}
