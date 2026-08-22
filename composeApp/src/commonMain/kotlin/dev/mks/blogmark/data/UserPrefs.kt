package dev.mks.blogmark.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * The first state in this app that is genuinely mutable and outlives a
 * composition, so it is also the first that needs an owner.
 *
 * It is a plain class rather than a ViewModel: there is no async work, no
 * lifecycle to survive beyond the process, and nothing to inject. Reads come
 * from an in-memory snapshot taken at construction; writes go straight through
 * to the store, so nothing can be lost by a process dying between the two.
 */
class UserPrefs(private val store: KeyValueStore) {
    var name: String? by mutableStateOf(store.getString(KeyName)?.takeIf { it.isNotBlank() })
        private set

    var introSeen: Boolean by mutableStateOf(store.getBoolean(KeyIntroSeen))
        private set

    /** A blank name is stored as absent, so "skip" and "cleared" mean the same thing. */
    fun updateName(value: String?) {
        val cleaned = value?.trim()?.takeIf { it.isNotEmpty() }
        name = cleaned
        store.putString(KeyName, cleaned)
    }

    fun markIntroSeen() {
        introSeen = true
        store.putBoolean(KeyIntroSeen, true)
    }

    /** Used by the "start over" affordance in settings, and by manual testing. */
    fun reset() {
        updateName(null)
        introSeen = false
        store.putBoolean(KeyIntroSeen, false)
    }

    private companion object {
        const val KeyName = "user.name"
        const val KeyIntroSeen = "intro.seen"
    }
}

@Composable
fun rememberUserPrefs(): UserPrefs {
    val store = rememberKeyValueStore()
    return remember(store) { UserPrefs(store) }
}
