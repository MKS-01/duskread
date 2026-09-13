package dev.mks.duskread.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A subscription Swift can end. Kotlin coroutines have no Obj-C representation, so a
 * `Job` cannot cross the bridge.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Bridges a [StateFlow] to a callback.
 */
internal fun <T> StateFlow<T>.watch(onEach: (T) -> Unit): Cancellable {
    val job = CoroutineScope(Dispatchers.Main).launch { collect { onEach(it) } }
    return object : Cancellable {
        override fun cancel() = job.cancel()
    }
}
