package dev.mks.duskread.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A subscription Swift can end.
 *
 * Kotlin coroutines have no Obj-C representation, so a `Job` cannot cross the
 * bridge. This is the smallest thing that can: Swift holds one per observation
 * and cancels it when the view goes away. Forgetting to is a leak that keeps
 * the collector — and so the graph — alive, which is why it is a type Swift has
 * to store rather than a fire-and-forget call.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Bridges a [StateFlow] to a callback.
 *
 * Main dispatcher rather than the default one because every caller is a SwiftUI
 * view updating its own state: hopping to a background thread only to hop back
 * would buy nothing and risk a frame's worth of tearing. Collection is
 * conflated by [StateFlow] itself, so a burst of writes delivers once.
 */
internal fun <T> StateFlow<T>.watch(onEach: (T) -> Unit): Cancellable {
    val job = CoroutineScope(Dispatchers.Main).launch { collect { onEach(it) } }
    return object : Cancellable {
        override fun cancel() = job.cancel()
    }
}
