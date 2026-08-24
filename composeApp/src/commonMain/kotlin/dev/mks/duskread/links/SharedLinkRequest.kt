package dev.mks.duskread.links

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A link shared into the app from outside, waiting to be saved.
 *
 * The same handoff shape as `HomeTabRequest`, and for the same reason:
 * `MainActivity` lives in the host `androidApp` module and is where the
 * `ACTION_SEND` intent arrives, but the library that stores links is
 * Compose-scoped state inside `HomeScreen`. Neither can call the other, so
 * this is the one point they meet.
 */
object SharedLinkRequest {
    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url

    fun offer(text: String) {
        extractUrl(text)?.let { _url.value = it }
    }

    fun consume() {
        _url.value = null
    }
}

/**
 * Pulls the URL out of shared text. Android's share sheet rarely hands over a
 * bare link — most apps send "Some article title https://example.com/x", and
 * some send the link with a trailing newline — so the whole payload cannot go
 * straight into the library.
 */
fun extractUrl(text: String): String? {
    val token = text.split(' ', '\n', '\t', '\r')
        .map { it.trim() }
        .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        ?: text.split(' ', '\n', '\t', '\r').map { it.trim() }.firstOrNull(::looksLikeUrl)

    // Trailing punctuation from prose ("read this: https://x.com/y.") would
    // otherwise become part of the URL.
    return token?.trimEnd('.', ',', ')', ']', '"', '\'')?.takeIf(::looksLikeUrl)
}
