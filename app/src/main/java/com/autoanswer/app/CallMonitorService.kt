package com.autoanswer.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast

class CallMonitorService : Service() {

    companion object {
        private const val TAG = "CallMonitorService"
        private const val CH_ID = "call_monitor"
        private const val NOTI_ID = 1001

        @Volatile
        var running = false

        fun start(c: Context) {
            val i = Intent(c, CallMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tm: TelephonyManager? = null
    private var listener: PhoneStateListener? = null
    private var lastIncomingNumber = ""
    private var ourAnswerPending = false
    private var ourCallActive = false

    override fun onCreate() {
        super.onCreate()
        running = true
        startForegroundCompat()
        RecWatcher.start(this)
        registerCallListener()
        Log.i(TAG, "service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running = false
        listener?.let { runCatching { tm?.listen(it, PhoneStateListener.LISTEN_NONE) } }
        RecWatcher.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CH_ID, "来电监听", NotificationManager.IMPORTANCE_LOW))
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CH_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        val noti = builder
            .setContentTitle("来电接听转发助手")
            .setContentText("后台监听中，白名单来电将自动接听并转发录音")
            .setSmallIcon(R.drawable.stat_notify_call)
            .setContentIntent(pi)
            .build()
        try {
            startForeground(NOTI_ID, noti)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    private fun registerCallListener() {
        tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                Log.i(TAG, "state=$state number=$phoneNumber")
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> onRinging(phoneNumber)
                    TelephonyManager.CALL_STATE_OFFHOOK -> onOffHook()
                    TelephonyManager.CALL_STATE_IDLE -> onIdle()
                }
            }
        }
        try {
            tm?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            Log.e(TAG, "missing READ_PHONE_STATE permission", e)
        }
    }

    private fun onRinging(number: String?) {
        lastIncomingNumber = number ?: ""
        if (!Prefs.isWhitelisted(this, number)) {
            Log.i(TAG, "not in whitelist, ignore: $number")
            return
        }
        ourAnswerPending = true
        val delayMs = Prefs.answerDelaySec(this) * 1000L
        Log.i(TAG, "whitelisted call from $number, auto answer in ${delayMs}ms")
        handler.postDelayed({
            if (!ourAnswerPending) return@postDelayed
            ourAnswerPending = false
            val svc = AutoAnswerService.instance
            if (svc == null) {
                Log.w(TAG, "accessibility service not enabled, cannot auto answer")
                toast("无障碍服务未开启，无法自动接听")
                return@postDelayed
            }
            val ok = svc.answerCall()
            Log.i(TAG, "dispatchGesture result=$ok")
            if (ok) ourCallActive = true
        }, delayMs)
    }

    private fun onOffHook() {
        // 接通后延迟开启免提，保证录音音量
        handler.postDelayed({
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= 31) {
                    val speaker = am.availableCommunicationDevices
                        .firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speaker != null) am.setCommunicationDevice(speaker)
                } else {
                    @Suppress("DEPRECATION") am.setSpeakerphoneOn(true)
                }
                Log.i(TAG, "speakerphone on")
            } catch (e: Exception) {
                Log.e(TAG, "enable speakerphone failed", e)
            }
        }, 800)
    }

    private fun onIdle() {
        ourAnswerPending = false
        if (ourCallActive) {
            ourCallActive = false
            val endAt = System.currentTimeMillis()
            Log.i(TAG, "our call ended, arm recorder watcher")
            RecWatcher.instance?.onCallEnded(lastIncomingNumber, endAt)
        }
    }

    private fun toast(msg: String) {
        handler.post { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }
}
