package com.autoanswer.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * 自动接听核心：通过 dispatchGesture 模拟上滑手势，适配澎湃OS / OriginOS 的滑动接听界面。
 * 注意：必须在系统设置 → 无障碍中手动开启本服务。
 */
class AutoAnswerService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoAnswerService"
        @Volatile
        var instance: AutoAnswerService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "accessibility service connected")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun answerCall(): Boolean {
        val disp = if (Build.VERSION.SDK_INT >= 30) {
            display
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        } ?: return false

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        disp.getRealMetrics(metrics)
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()

        val path = Path()
        path.moveTo(w / 2f, h * 0.82f)
        path.lineTo(w / 2f, h * 0.30f)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.i(TAG, "answer gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "answer gesture cancelled")
            }
        }, null)
    }
}
