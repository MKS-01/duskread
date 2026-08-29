package dev.mks.duskread.data

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

/**
 * The same store, reachable without composition.
 *
 * Everything in the app proper goes through [rememberKeyValueStore], but the
 * home-screen widget and the Pomodoro service both need to read and write
 * this state from outside any Compose tree. They must land in the *same*
 * preferences file as the app, so the file name lives here and only here —
 * two spellings of it would be two silently separate stores.
 *
 * ("algo_atlas" is a pre-rename holdover. Renaming it now would orphan every
 * existing reader's saved links for no gain.)
 */
fun keyValueStore(context: Context): KeyValueStore = AndroidStore(context.getSharedPreferences("algo_atlas", Context.MODE_PRIVATE))

@Composable
actual fun rememberKeyValueStore(): KeyValueStore {
    val context = LocalContext.current
    return remember(context) { keyValueStore(context) }
}
