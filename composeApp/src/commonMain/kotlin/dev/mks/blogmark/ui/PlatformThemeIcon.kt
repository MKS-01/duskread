package dev.mks.blogmark.ui

import androidx.compose.runtime.Composable

/**
 * Keeps the OS-level launcher icon and splash screen in step with [mono],
 * on platforms where those live outside Compose's reach and can only be
 * switched at the manifest/component level (Android's activity-alias pair —
 * see `PlatformThemeIcon.android.kt`). Everywhere else there is nothing to
 * sync, since the icon and splash aren't theme-aware to begin with.
 */
@Composable
expect fun PlatformThemeIcon(mono: Boolean)
