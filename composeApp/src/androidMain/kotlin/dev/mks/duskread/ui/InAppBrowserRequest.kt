package dev.mks.duskread.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * An article waiting to be shown in the embedded, force-darkened reader browser, with the
 * list it came from so the reader can turn to the next one.
 */
object InAppBrowserRequest {
    private val _queue = MutableStateFlow<ReadingQueue?>(null)
    val queue: StateFlow<ReadingQueue?> = _queue

    fun open(queue: ReadingQueue) {
        _queue.value = queue
    }

    fun consume() {
        _queue.value = null
    }
}
