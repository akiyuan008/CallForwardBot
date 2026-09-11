package com.autoanswer.app

import android.util.Base64

/** 轻量混淆（XOR + Base64），防止明文躺在 SharedPreferences 里，不是强加密。 */
object Crypto {
    private val KEY = "CallForwardBot2026!".toByteArray(Charsets.UTF_8)

    fun enc(plain: String): String {
        val data = plain.toByteArray(Charsets.UTF_8)
        for (i in data.indices) data[i] = (data[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    fun dec(encoded: String): String {
        return try {
            val data = Base64.decode(encoded, Base64.NO_WRAP)
            for (i in data.indices) data[i] = (data[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
            String(data, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
