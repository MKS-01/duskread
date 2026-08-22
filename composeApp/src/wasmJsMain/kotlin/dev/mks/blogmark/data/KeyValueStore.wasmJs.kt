package dev.mks.blogmark.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.localStorage

private class WebStore : KeyValueStore {
    override fun getString(key: String): String? = localStorage.getItem(key)

    override fun putString(key: String, value: String?) {
        if (value == null) localStorage.removeItem(key) else localStorage.setItem(key, value)
    }
}

@Composable
actual fun rememberKeyValueStore(): KeyValueStore = remember { WebStore() }
