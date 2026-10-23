package app.wfcwatch

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var stateView: TextView
    private lateinit var detailView: TextView
    private lateinit var iconView: android.widget.ImageView
    private lateinit var permButton: Button
    private lateinit var batteryButton: Button
    private lateinit var monitorSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stateView = findViewById(R.id.state)
        detailView = findViewById(R.id.detail)
        iconView = findViewById(R.id.icon)
        permButton = findViewById(R.id.btn_permissions)
        batteryButton = findViewById(R.id.btn_battery)
        monitorSwitch = findViewById(R.id.switch_monitor)

        findViewById<Button>(R.id.btn_open).setOnClickListener { SettingsLauncher.open(this) }
        findViewById<Button>(R.id.btn_refresh).setOnClickListener { refresh() }
        permButton.setOnClickListener { askPermissions() }
        batteryButton.setOnClickListener { askBatteryExemption() }

        monitorSwitch.isChecked = isMonitorEnabled(this)
        monitorSwitch.setOnCheckedChangeListener { _, on ->
            prefs(this).edit().putBoolean(KEY_MONITOR, on).apply()
            if (on) WfcMonitorService.start(this)
            else stopService(Intent(this, WfcMonitorService::class.java))
        }

        if (!WfcStatus.hasPhonePermission(this)) askPermissions()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (isMonitorEnabled(this)) WfcMonitorService.start(this)
    }

    private fun refresh() {
        val r = WfcStatus.refreshEverywhere(this)
        when (r.state) {
            WfcStatus.State.ON -> {
                stateView.text = "On"
                stateView.setTextColor(Color.parseColor("#7CF29A"))
            }
            WfcStatus.State.OFF -> {
                stateView.text = "Off"
                stateView.setTextColor(Color.parseColor("#FF8A80"))
            }
            WfcStatus.State.NO_PERMISSION -> {
                stateView.text = "Setup"
                stateView.setTextColor(Color.parseColor("#FFE082"))
            }
        }
        detailView.text = r.detail
        iconView.setImageResource(
            if (r.state == WfcStatus.State.ON) R.drawable.ic_wfc else R.drawable.ic_wfc_off
        )
        iconView.setColorFilter(stateView.currentTextColor)

        permButton.visibility =
            if (WfcStatus.hasPhonePermission(this) && notificationsAllowed()) View.GONE else View.VISIBLE
        batteryButton.visibility =
            if (isIgnoringBatteryOptimizations()) View.GONE else View.VISIBLE
    }

    private fun askPermissions() {
        val perms = mutableListOf(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(perms.toTypedArray(), REQ_PERMS)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        refresh()
        if (isMonitorEnabled(this)) WfcMonitorService.start(this)
    }

    private fun notificationsAllowed() =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun isIgnoringBatteryOptimizations(): Boolean =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    @SuppressLint("BatteryLife")
    private fun askBatteryExemption() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    companion object {
        private const val REQ_PERMS = 10
        private const val KEY_MONITOR = "monitor"
        private fun prefs(ctx: Context) = ctx.getSharedPreferences("wfc", Context.MODE_PRIVATE)
        fun isMonitorEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_MONITOR, true)
    }
}
