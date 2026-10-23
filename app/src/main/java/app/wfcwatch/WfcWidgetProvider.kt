package app.wfcwatch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class WfcWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        updateAll(ctx, WfcStatus.check(ctx))
    }

    override fun onEnabled(ctx: Context) {
        // First widget placed: make sure the background monitor is running.
        WfcMonitorService.start(ctx)
    }

    companion object {
        fun updateAll(ctx: Context, r: WfcStatus.Result) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, WfcWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val views = RemoteViews(ctx.packageName, R.layout.widget_wfc)
            val (label, bg) = when (r.state) {
                WfcStatus.State.ON -> "ON" to R.drawable.widget_bg_on
                WfcStatus.State.OFF -> "OFF" to R.drawable.widget_bg_off
                WfcStatus.State.NO_PERMISSION -> "SETUP" to R.drawable.widget_bg_unknown
            }
            views.setImageViewResource(
                R.id.widget_icon,
                if (r.state == WfcStatus.State.ON) R.drawable.ic_wfc else R.drawable.ic_wfc_off
            )
            views.setTextViewText(R.id.widget_state, label)
            views.setInt(R.id.widget_root, "setBackgroundResource", bg)

            // Permission missing -> open the app; otherwise jump to Wi-Fi Calling settings.
            val target = if (r.state == WfcStatus.State.NO_PERMISSION)
                Intent(ctx, MainActivity::class.java)
            else
                Intent(ctx, OpenWfcSettingsActivity::class.java)
            val pi = PendingIntent.getActivity(
                ctx, 0, target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            mgr.updateAppWidget(ids, views)
        }
    }
}
