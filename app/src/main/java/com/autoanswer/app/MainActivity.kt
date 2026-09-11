package com.autoanswer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissionsIfNeeded()

        val pager = findViewById<ViewPager2>(R.id.viewPager)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> SettingsFragment()
                1 -> WhitelistFragment()
                2 -> HistoryFragment()
                3 -> CheckFragment()
                else -> SettingsFragment()
            }
        }
        TabLayoutMediator(findViewById<TabLayout>(R.id.tabs), pager) { tab, pos ->
            tab.text = arrayOf("设置", "白名单", "历史", "自检")[pos]
        }.attach()

        CallMonitorService.start(this)
        KeepAliveJob.schedule(this)
        promptAccessibilityIfDisabled()
    }

    private fun requestPermissionsIfNeeded() {
        val need = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT <= 32 &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)
    }

    private fun promptAccessibilityIfDisabled() {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
        if (!enabled) {
            Toast.makeText(this, "请到 设置 → 无障碍 中开启「来电接听转发助手」，否则无法自动接听", Toast.LENGTH_LONG).show()
            runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
    }
}
