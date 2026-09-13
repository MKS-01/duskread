package dev.mks.duskread.data

import androidx.compose.runtime.Composable

/**
 * The smallest persistence surface that does the job.
 */
interface KeyValueStore {
    fun getString(key: String): String?

    fun putString(key: String, value: String?)

    /**
     * [fallback] rather than `default` because iOS implements this interface in Swift,
     * where `default` is a keyword and would arrive needing backticks at every call site.
     */
    fun getBoolean(key: String, fallback: Boolean = false): Boolean = getString(key)?.toBooleanStrictOrNull() ?: fallback

    fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())
}

/**
 * Composable for the same reason as `rememberUrlOpener`: Android needs the local
 * `Context`, and that is only reachable from composition.
 */
@Composable
expect fun rememberKeyValueStore(): KeyValueStore
