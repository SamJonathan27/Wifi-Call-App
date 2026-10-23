package app.wfcwatch

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast

/**
 * Opens the Wi-Fi Calling settings screen.
 *
 * There is no single public "open Wi-Fi Calling" shortcut that works on every
 * phone, so this tries the most direct screens first and falls back to the
 * Phone app's call settings (where Samsung lists "Wi-Fi calling").
 */
object SettingsLauncher {

    private val candidates: List<Pair<String, () -> Intent>> = listOf(
        // Android's own Wi-Fi Calling screen (present on many phones).
        "Wi-Fi Calling" to { Intent("android.settings.WIFI_CALLING_SETTINGS") },
        "Wi-Fi Calling" to {
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$WifiCallingSettingsActivity"
                )
            )
        },
        // Samsung One UI keeps Wi-Fi Calling inside the Phone app's settings.
        "Call settings" to { Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS) },
        // Last resort: Connections / network settings.
        "Network settings" to { Intent(Settings.ACTION_WIRELESS_SETTINGS) },
    )

    /** Returns the name of the screen that opened, or null if none did. */
    fun open(ctx: Context): String? {
        for ((label, build) in candidates) {
            val intent = build().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                ctx.startActivity(intent)
                if (label != "Wi-Fi Calling") {
                    Toast.makeText(
                        ctx,
                        "Opened $label — tap \"Wi-Fi calling\" and switch it on",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return label
            } catch (_: ActivityNotFoundException) {
                // try the next one
            } catch (_: SecurityException) {
                // screen exists but isn't open to other apps; try the next one
            }
        }
        Toast.makeText(ctx, "Couldn't open settings on this phone", Toast.LENGTH_LONG).show()
        return null
    }
}
