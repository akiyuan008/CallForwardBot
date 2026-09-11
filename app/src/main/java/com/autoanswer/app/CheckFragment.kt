package com.autoanswer.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class CheckFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_check, container, false)
        val tv = v.findViewById<TextView>(R.id.tvCheckResult)
        v.findViewById<Button>(R.id.btnRunCheck).setOnClickListener {
            tv.text = "自检中……"
            Thread {
                val results = SelfCheck.runChecks(requireContext())
                val report = SelfCheck.report(results) + "\n\n录音目录列表：\n" +
                        RecWatcher.candidateDirs().joinToString("\n") { it.absolutePath }
                activity?.runOnUiThread { tv.text = report }
            }.start()
        }
        v.findViewById<Button>(R.id.btnRunCheck).setOnLongClickListener {
            RecWatcher.instance?.forceScanAndSend()
            tv.text = "已触发一次强制扫描上传（模拟 M4 链路）"
            true
        }
        return v
    }
}
