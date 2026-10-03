package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.mks.duskread.db.DuskReadDatabase

// Lazily, once: the SwiftUI shell and any Compose host share one connection pool.
private val shared: DuskReadDatabase by lazy { DuskReadDatabase(NativeSqliteDriver(DuskReadDatabase.Schema, "duskread.db")) }

/** The database for the bridge, which builds its graph outside composition. */
fun duskReadDatabase(): DuskReadDatabase = shared

@Composable
actual fun rememberDatabase(): DuskReadDatabase = shared
