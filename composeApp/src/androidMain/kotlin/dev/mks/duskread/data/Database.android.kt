package dev.mks.duskread.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.mks.duskread.db.DuskReadDatabase

// One per process: the app and its widgets share the file, and a second open connection
// to it would only contend with the first.
private var shared: DuskReadDatabase? = null

/** The same database, reachable without composition — the widgets read it. */
@Synchronized
fun duskReadDatabase(context: Context): DuskReadDatabase = shared ?: DuskReadDatabase(
    AndroidSqliteDriver(DuskReadDatabase.Schema, context.applicationContext, "duskread.db"),
).also { shared = it }

@Composable
actual fun rememberDatabase(): DuskReadDatabase = duskReadDatabase(LocalContext.current)
