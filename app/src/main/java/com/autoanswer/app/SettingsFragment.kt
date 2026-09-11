package com.autoanswer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_settings, container, false)
        val c = requireContext()

        val etCorpId = v.findViewById<EditText>(R.id.etCorpId)
        val etSecret = v.findViewById<EditText>(R.id.etSecret)
        val etAgentId = v.findViewById<EditText>(R.id.etAgentId)
        val etToUser = v.findViewById<EditText>(R.id.etToUser)
        val etDelay = v.findViewById<EditText>(R.id.etDelay)

        etCorpId.setText(Prefs.corpId(c))
        etSecret.setText(Prefs.corpSecret(c))
        etAgentId.setText(Prefs.agentId(c))
        etToUser.setText(Prefs.toUser(c))
        etDelay.setText(Prefs.answerDelaySec(c).toString())

        v.findViewById<Button>(R.id.btnSave).setOnClickListener {
            Prefs.setCorpId(c, etCorpId.text.toString())
            Prefs.setCorpSecret(c, etSecret.text.toString())
            Prefs.setAgentId(c, etAgentId.text.toString())
            Prefs.setToUser(c, etToUser.text.toString())
            Prefs.setAnswerDelaySec(c, etDelay.text.toString().toIntOrNull() ?: 3)
            Toast.makeText(c, "已保存", Toast.LENGTH_SHORT).show()
        }

        v.findViewById<Button>(R.id.btnTestSend).setOnClickListener { btn ->
            btn.isEnabled = false
            WeComSender.sendTextTest(c) { ok, detail ->
                activity?.runOnUiThread {
                    btn.isEnabled = true
                    Toast.makeText(c, if (ok) "发送成功 $detail" else "发送失败：$detail", Toast.LENGTH_LONG).show()
                }
            }
        }

        v.findViewById<Button>(R.id.btnBattery).setOnClickListener {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${c.packageName}")
            )
            runCatching { startActivity(intent) }
        }
        return v
    }
}
