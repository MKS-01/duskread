package dev.mks.duskread.data

import androidx.compose.runtime.Composable

/**
 * The smallest persistence surface that does the job.
 *
 * Everything this app needs to remember — a name, whether the intro has been
 * seen, the saved links themselves — is a few kilobytes of key/value state.
 * That does not warrant a database: there is
 * nothing to query, nothing to join, and the whole set fits in memory many
 * times over. It also keeps every target: Room publishes no Wasm artifact, so
 * adopting it would quietly drop the web build.
 *
 * Reads are synchronous on purpose. The alternative is an async load, which
 * means a frame where we do not yet know whether to show the intro — and a
 * returning reader would see it flash. At this size, synchronous is both
 * simpler and better behaved.
 */
interface KeyValueStore {
    fun getString(key: String): String?

    fun putString(key: String, value: String?)

    fun getBoolean(key: String, default: Boolean = false): Boolean = getString(key)?.toBooleanStrictOrNull() ?: default

    fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())
}

/**
 * Composable for the same reason as `rememberUrlOpener`: Android needs the
 * local `Context`, and that is only reachable from composition.
 */
@Composable
expect fun rememberKeyValueStore(): KeyValueStore
