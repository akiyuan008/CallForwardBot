package com.autoanswer.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SelfCheck {

    fun runChecks(c: Context): List<Pair<String, Boolean>> {
        val results = mutableListOf<Pair<String, Boolean>>()

        results.add("READ_PHONE_STATE 权限" to hasPerm(c, android.Manifest.permission.READ_PHONE_STATE))
        if (Build.VERSION.SDK_INT >= 33) {
            results.add("通知权限" to hasPerm(c, android.Manifest.permission.POST_NOTIFICATIONS))
        }
        if (Build.VERSION.SDK_INT <= 32) {
            results.add("存储读取权限" to hasPerm(c, android.Manifest.permission.READ_EXTERNAL_STORAGE))
        }
        results.add("无障碍服务已开启" to isAccessibilityEnabled(c))
        results.add("前台服务运行中" to CallMonitorService.running)
        results.add("白名单号码数：${Prefs.whitelist(c).size}" to Prefs.whitelist(c).isNotEmpty())
        results.add("自动接听延迟：${Prefs.answerDelaySec(c)} 秒" to true)

        val hasCorp = Prefs.corpId(c).isNotEmpty() && Prefs.corpSecret(c).isNotEmpty() &&
                Prefs.agentId(c).isNotEmpty() && Prefs.toUser(c).isNotEmpty()
        results.add("企业微信参数完整" to hasCorp)
        if (hasCorp) {
            val ok = runBlocking {
                try {
                    withContext(Dispatchers.IO) { WeComApi.getToken(c) }
                    true
                } catch (e: Exception) {
                    LogHolder.lastError = e.message ?: e.toString()
                    false
                }
            }
            results.add("企业微信 gettoken 成功" to ok)
        }
        results.add("录音目录可访问（${RecWatcher.candidateDirs().size} 个）" to RecWatcher.candidateDirs().isNotEmpty())
        results.add("已忽略电池优化" to isBatteryIgnored(c))
        return results
    }

    object LogHolder {
        var lastError = ""
    }

    private fun hasPerm(c: Context, p: String) =
        c.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    private fun isBatteryIgnored(c: Context): Boolean {
        val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(c.packageName)
    }

    private fun isAccessibilityEnabled(c: Context): Boolean {
        val am = c.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == c.packageName }
    }

    fun report(results: List<Pair<String, Boolean>>): String {
        val sb = StringBuilder()
        sb.append("自检时间：")
            .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date()))
            .append("

")
        results.forEach { (name, ok) ->
            sb.append(if (ok) "✅ " else "❌ ").append(name).append("
")
        }
        if (LogHolder.lastError.isNotEmpty()) {
            sb.append("
最近错误：").append(LogHolder.lastError)
        }
        return sb.toString()
    }
}
