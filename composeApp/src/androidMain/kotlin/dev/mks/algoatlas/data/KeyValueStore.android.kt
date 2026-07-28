package dev.mks.algoatlas.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `SharedPreferences` rather than DataStore. DataStore is the modern answer
 * for anything sizeable, but it is asynchronous and would add a dependency to
 * store two strings. Once loaded, `SharedPreferences` reads come from memory.
 */
private class AndroidStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

@Composable
actual fun rememberKeyValueStore(): KeyValueStore {
    val context = LocalContext.current
    return remember(context) {
        AndroidStore(context.getSharedPreferences("algo_atlas", Context.MODE_PRIVATE))
    }
}
