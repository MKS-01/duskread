package dev.mks.duskread.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A URL waiting to be shown in the embedded browser.
 *
 * The same handoff Android uses, for the same reason: [rememberUrlOpener] is
 * called from deep inside a link row, and the one place that can lay a
 * full-window browser over everything else is `PlatformOverlay`, several
 * screens above it. Duplicated in this source set rather than lifted into
 * `commonMain` because iOS and web have no browser to hand a request to —
 * a shared object would be a queue with nothing at the other end on two of
 * the four targets.
 */
object InAppBrowserRequest {
    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url

    fun open(url: String) {
        _url.value = url
    }

    fun consume() {
        _url.value = null
    }
}
