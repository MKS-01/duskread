package dev.mks.duskread.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

private const val LauncherPaper = "dev.mks.duskread.android.LauncherPaper"
private const val LauncherInk = "dev.mks.duskread.android.LauncherInk"

/**
 * Flips which of the two launcher aliases (declared in AndroidManifest.xml)
 * is enabled, so the home-screen icon — and, through that alias's own
 * android:theme, the splash it launches into — matches [mono]. DONT_KILL_APP
 * matters here: without it the system would kill this very process to apply
 * the change, which would otherwise fire mid-session every time the reader
 * taps the theme toggle.
 */
@Composable
actual fun PlatformThemeIcon(mono: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(mono) {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val enabled = ComponentName(packageName, if (mono) LauncherInk else LauncherPaper)
        val disabled = ComponentName(packageName, if (mono) LauncherPaper else LauncherInk)
        packageManager.setComponentEnabledSetting(
            enabled,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        packageManager.setComponentEnabledSetting(
            disabled,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
