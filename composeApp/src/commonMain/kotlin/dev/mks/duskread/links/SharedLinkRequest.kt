package dev.mks.duskread.links

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A link shared into the app from outside, waiting to be saved.
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
 * Pulls the URL out of shared text.
 */
fun extractUrl(text: String): String? {
    val token = text.split(' ', '\n', '\t', '\r')
        .map { it.trim() }
        .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        ?: text.split(' ', '\n', '\t', '\r').map { it.trim() }.firstOrNull(::looksLikeUrl)

    // Trailing punctuation from prose ("read this: https://x.com/y.") would otherwise
    // become part of the URL.
    return token?.trimEnd('.', ',', ')', ']', '"', '\'')?.takeIf(::looksLikeUrl)
}
