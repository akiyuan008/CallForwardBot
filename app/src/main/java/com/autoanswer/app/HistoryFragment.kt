package com.autoanswer.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_list, container, false)
        val c = requireContext()
        val listView = v.findViewById<ListView>(R.id.listView)
        val btnPrimary = v.findViewById<View>(R.id.btnPrimary)
        val btnClear = v.findViewById<View>(R.id.btnClear)
        btnPrimary.visibility = View.GONE
        btnClear.visibility = View.VISIBLE

        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)

        fun render() {
            val arr = Prefs.history(c)
            val items = mutableListOf<Pair<JSONObject, String>>()
            for (i in arr.length() - 1 downTo 0) {
                val o = arr.getJSONObject(i)
                val ok = o.optBoolean("ok")
                val line = (if (ok) "✅ " else "❌ ") +
                        fmt.format(Date(o.optLong("time"))) +
                        "  来自：" + o.optString("number") +
                        "  大小：" + (o.optLong("size") / 1024) + "KB
" +
                        o.optString("detail")
                items.add(o to line)
            }
            listView.adapter = ArrayAdapter(c, android.R.layout.simple_list_item_1, items.map { it.second })
            listView.tag = items
        }
        render()

        listView.setOnItemClickListener { _, _, position, _ ->
            val items = listView.tag as List<Pair<JSONObject, String>>
            val (o, _) = items[position]
            val file = File(o.optString("file"))
            AlertDialog.Builder(c)
                .setTitle("重发这条录音？")
                .setMessage("文件：${file.name}
存在：${file.exists()}")
                .setPositiveButton("重发") { _, _ ->
                    if (file.exists()) {
                        WeComSender.sendRecording(c, o.optString("number"), file) { ok, detail ->
                            activity?.runOnUiThread {
                                Toast.makeText(c, if (ok) "重发成功" else "重发失败：$detail", Toast.LENGTH_LONG).show()
                                render()
                            }
                        }
                    } else {
                        Toast.makeText(c, "录音文件已被清理，无法重发", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        btnClear.setOnClickListener {
            Prefs.clearHistory(c)
            render()
        }
        return v
    }
}
