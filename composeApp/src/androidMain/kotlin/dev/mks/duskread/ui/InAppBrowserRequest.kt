package dev.mks.duskread.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A URL waiting to be shown in the embedded, force-darkened reader browser.
 *
 * [rememberUrlOpener] is called from deep inside link and feed-card
 * composables — nowhere near `MainActivity`, which is the one place that can
 * lay a full-viewport [InAppBrowserScreen] over everything else. Same
 * handoff shape as `SharedLinkRequest` and `HomeTabRequest`, for the same
 * reason: the trigger and the host have no other way to reach each other.
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
