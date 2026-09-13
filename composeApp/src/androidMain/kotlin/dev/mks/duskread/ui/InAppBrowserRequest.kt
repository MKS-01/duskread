package dev.mks.duskread.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A URL waiting to be shown in the embedded, force-darkened reader browser.
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
