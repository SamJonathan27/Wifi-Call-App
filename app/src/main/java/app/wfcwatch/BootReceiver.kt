package app.wfcwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the monitor after the phone reboots or the app is updated. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (MainActivity.isMonitorEnabled(ctx)) WfcMonitorService.start(ctx)
        WfcStatus.refreshEverywhere(ctx)
    }
}
