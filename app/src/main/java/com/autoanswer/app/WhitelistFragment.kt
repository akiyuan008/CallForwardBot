package com.autoanswer.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment

class WhitelistFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_list, container, false)
        val c = requireContext()
        val listView = v.findViewById<ListView>(R.id.listView)
        val btnAdd = v.findViewById<View>(R.id.btnPrimary)
        val btnClear = v.findViewById<View>(R.id.btnClear)
        btnAdd.visibility = View.VISIBLE
        btnClear.visibility = View.VISIBLE

        fun refresh() {
            val items = Prefs.whitelist(c).sorted()
            listView.adapter = ArrayAdapter(c, android.R.layout.simple_list_item_1, items)
        }
        refresh()

        btnAdd.setOnClickListener {
            val input = EditText(c)
            input.hint = "号码，支持前缀如 0755"
            AlertDialog.Builder(c)
                .setTitle("添加白名单号码")
                .setView(input)
                .setPositiveButton("添加") { _, _ ->
                    val num = input.text.toString().trim()
                    if (num.isEmpty()) return@setPositiveButton
                    Prefs.setWhitelist(c, Prefs.whitelist(c) + num)
                    refresh()
                    Toast.makeText(c, "已添加 $num", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val num = (listView.adapter.getItem(position) as String)
            AlertDialog.Builder(c)
                .setTitle("删除号码")
                .setMessage("确定删除 $num ？")
                .setPositiveButton("删除") { _, _ ->
                    Prefs.setWhitelist(c, Prefs.whitelist(c) - num)
                    refresh()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        btnClear.setOnClickListener {
            AlertDialog.Builder(c)
                .setTitle("清空白名单")
                .setMessage("确定删除全部号码？")
                .setPositiveButton("清空") { _, _ ->
                    Prefs.setWhitelist(c, emptySet())
                    refresh()
                }
                .setNegativeButton("取消", null)
                .show()
        }
        return v
    }
}
