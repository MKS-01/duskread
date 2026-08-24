package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.util.Properties

/**
 * A properties file under the user's home directory. Written on every change —
 * the file is a handful of lines, so there is nothing to batch.
 */
private class DesktopStore(private val file: File) : KeyValueStore {
    private val props = Properties().apply {
        if (file.exists()) file.inputStream().use { load(it) }
    }

    override fun getString(key: String): String? = props.getProperty(key)

    override fun putString(key: String, value: String?) {
        if (value == null) props.remove(key) else props.setProperty(key, value)
        runCatching {
            file.parentFile?.mkdirs()
            file.outputStream().use { props.store(it, "DuskRead") }
        }
    }
}

@Composable
actual fun rememberKeyValueStore(): KeyValueStore = remember {
    DesktopStore(File(System.getProperty("user.home"), ".algoatlas/prefs.properties"))
}
