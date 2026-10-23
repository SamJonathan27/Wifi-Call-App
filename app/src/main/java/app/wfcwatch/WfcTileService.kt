package app.wfcwatch

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile (swipe-down panel): shows ON/OFF, tap opens the settings. */
class WfcTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val r = WfcStatus.check(this)
        qsTile?.apply {
            state = if (r.state == WfcStatus.State.ON) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_label)
            icon = android.graphics.drawable.Icon.createWithResource(
                this@WfcTileService,
                if (r.state == WfcStatus.State.ON) R.drawable.ic_wfc else R.drawable.ic_wfc_off
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = when (r.state) {
                    WfcStatus.State.ON -> "On"
                    WfcStatus.State.OFF -> "Off"
                    WfcStatus.State.NO_PERMISSION -> "Setup"
                }
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, OpenWfcSettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pi = PendingIntent.getActivity(
                this, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        fun requestRefresh(ctx: Context) {
            try {
                requestListeningState(ctx, ComponentName(ctx, WfcTileService::class.java))
            } catch (_: Exception) {
                // Tile not added to the panel; nothing to refresh.
            }
        }
    }
}
