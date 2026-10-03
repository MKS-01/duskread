package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import dev.mks.duskread.db.DuskReadDatabase

/**
 * The on-device SQLite database. Composable for the same reason as the key-value store:
 * Android needs a `Context` to find the file.
 */
@Composable
expect fun rememberDatabase(): DuskReadDatabase
