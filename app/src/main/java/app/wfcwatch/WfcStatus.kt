package app.wfcwatch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/**
 * Works out whether Wi-Fi Calling is actually working right now.
 *
 * Android does not let ordinary apps read the Wi-Fi Calling *switch* itself
 * (that needs a system-only permission). What it does expose is the network the
 * phone's voice service is registered on. When Wi-Fi Calling is on and working,
 * that network is IWLAN ("voice over Wi-Fi"). With no cellular signal at all,
 * anything other than IWLAN means you can't receive calls — which is exactly
 * what you care about.
 */
object WfcStatus {

    enum class State { ON, OFF, NO_PERMISSION }

    data class Result(
        val state: State,
        /** Short human-readable reason, shown under the status. */
        val detail: String,
        /** Raw voice network type(s), shown in the app for troubleshooting. */
        val debug: String = ""
    )

    fun hasPhonePermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun check(ctx: Context): Result {
        if (!hasPhonePermission(ctx)) {
            return Result(State.NO_PERMISSION, "Open the app to grant phone permission")
        }

        val wifiUp = isWifiConnected(ctx)
        val baseTm = ctx.getSystemService(TelephonyManager::class.java)
        val types = mutableListOf<Pair<Int, Int>>() // subId to voice network type

        try {
            // Check every active SIM (dual-SIM phones), not just the default one.
            val sm = ctx.getSystemService(SubscriptionManager::class.java)
            val subs = sm?.activeSubscriptionInfoList.orEmpty()
            if (subs.isEmpty()) {
                types += -1 to baseTm.voiceNetworkType
            } else {
                for (info in subs) {
                    val tm = baseTm.createForSubscriptionId(info.subscriptionId)
                    types += info.subscriptionId to tm.voiceNetworkType
                }
            }
        } catch (e: SecurityException) {
            return Result(State.NO_PERMISSION, "Open the app to grant phone permission")
        }

        val debug = types.joinToString { (sub, t) ->
            (if (sub >= 0) "SIM $sub: " else "") + networkTypeName(t)
        }
        val onWifiCalling = types.any { it.second == TelephonyManager.NETWORK_TYPE_IWLAN }

        return when {
            onWifiCalling -> Result(State.ON, "Calls are coming in over Wi-Fi", debug)
            !wifiUp -> Result(State.OFF, "Wi-Fi is disconnected", debug)
            else -> Result(State.OFF, "Wi-Fi Calling is off or not registered", debug)
        }
    }

    private fun isWifiConnected(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.allNetworks.any { n ->
            cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    private fun networkTypeName(t: Int): String = when (t) {
        TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN (Wi-Fi Calling)"
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_GSM -> "2G"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "No service"
        else -> "type $t"
    }

    /** Push the latest status to every place that shows it. */
    fun refreshEverywhere(ctx: Context): Result {
        val r = check(ctx)
        WfcWidgetProvider.updateAll(ctx, r)
        WfcTileService.requestRefresh(ctx)
        return r
    }
}
