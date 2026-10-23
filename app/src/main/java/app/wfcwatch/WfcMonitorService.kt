package app.wfcwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Background watcher. Re-checks Wi-Fi Calling every 30 seconds and whenever
 * Wi-Fi or the screen changes, keeps the widget/tile current, and raises a
 * loud alert as soon as Wi-Fi Calling drops.
 */
class WfcMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastState: WfcStatus.State? = null
    private var lastAlertAt = 0L

    private val poll = object : Runnable {
        override fun run() {
            checkNow()
            handler.postDelayed(this, POLL_MS)
        }
    }

    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = recheckSoon()
        override fun onLost(network: Network) = recheckSoon()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = recheckSoon()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startInForeground(WfcStatus.check(this))
        getSystemService(ConnectivityManager::class.java)
            ?.registerDefaultNetworkCallback(netCallback)
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        handler.post(poll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        recheckSoon()
        return START_STICKY // ask Android to restart us if we get killed
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(netCallback)
        } catch (_: Exception) {}
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    /** Wi-Fi Calling takes a few seconds to (re)register after Wi-Fi changes. */
    private fun recheckSoon() {
        handler.postDelayed({ checkNow() }, 3_000)
        handler.postDelayed({ checkNow() }, 15_000)
    }

    private fun checkNow() {
        val r = WfcStatus.refreshEverywhere(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(ONGOING_ID, buildOngoing(r))

        val now = System.currentTimeMillis()
        when (r.state) {
            WfcStatus.State.OFF -> {
                val justDropped = lastState != WfcStatus.State.OFF
                val stillOffReminder = now - lastAlertAt > REMIND_MS
                if (justDropped || stillOffReminder) {
                    nm.notify(ALERT_ID, buildAlert(r))
                    lastAlertAt = now
                }
            }
            else -> nm.cancel(ALERT_ID)
        }
        lastState = r.state
    }

    private fun openSettingsIntent(): PendingIntent = PendingIntent.getActivity(
        this, 2,
        Intent(this, OpenWfcSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildOngoing(r: WfcStatus.Result): Notification {
        val title = when (r.state) {
            WfcStatus.State.ON -> "Wi-Fi Calling: ON"
            WfcStatus.State.OFF -> "Wi-Fi Calling: OFF"
            WfcStatus.State.NO_PERMISSION -> "Wi-Fi Calling: setup needed"
        }
        return Notification.Builder(this, CH_STATUS)
            .setSmallIcon(if (r.state == WfcStatus.State.ON) R.drawable.ic_wfc else R.drawable.ic_wfc_off)
            .setContentTitle(title)
            .setContentText(r.detail)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openSettingsIntent())
            .build()
    }

    private fun buildAlert(r: WfcStatus.Result): Notification =
        Notification.Builder(this, CH_ALERT)
            .setSmallIcon(R.drawable.ic_wfc_off)
            .setContentTitle("Wi-Fi Calling is OFF — you can't receive calls")
            .setContentText(r.detail + ". Tap to turn it back on.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openSettingsIntent())
            .build()

    private fun startInForeground(r: WfcStatus.Result) {
        val n = buildOngoing(r)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ONGOING_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ONGOING_ID, n)
        }
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "Status (always shown)", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Wi-Fi Calling turned off", NotificationManager.IMPORTANCE_HIGH)
                .apply { enableVibration(true) }
        )
    }

    companion object {
        private const val CH_STATUS = "status"
        private const val CH_ALERT = "alert"
        private const val ONGOING_ID = 1
        private const val ALERT_ID = 2
        private const val POLL_MS = 30_000L
        private const val REMIND_MS = 15 * 60_000L // re-alert every 15 min while still off

        fun start(ctx: Context) {
            if (!WfcStatus.hasPhonePermission(ctx)) return
            val i = Intent(ctx, WfcMonitorService::class.java)
            try {
                ctx.startForegroundService(i)
            } catch (_: Exception) {
                // Android may refuse a background start; the app or widget will retry.
            }
        }
    }
}
