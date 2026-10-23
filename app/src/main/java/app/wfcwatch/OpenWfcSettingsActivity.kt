package app.wfcwatch

import android.app.Activity
import android.os.Bundle

/** Invisible hop used by the widget, tile and alert notification. */
class OpenWfcSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsLauncher.open(this)
        finish()
    }
}
