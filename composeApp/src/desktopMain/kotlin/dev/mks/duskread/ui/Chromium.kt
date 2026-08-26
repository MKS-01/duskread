package dev.mks.duskread.ui

import dev.datlag.kcef.KCEF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * The embedded browser's lifecycle, which on the JVM is a much bigger thing
 * than on Android.
 *
 * Android hands every app a `WebView` that is already installed, already
 * running and already updated. The JVM hands you nothing, so DuskRead brings
 * its own Chromium: KCEF downloads a CEF bundle on first use, unpacks it
 * next to the app's data, and keeps it for every launch after. That is a
 * couple of hundred megabytes and a minute or two of waiting, exactly once.
 *
 * Started lazily on the first link opened rather than at launch, so a session
 * that never opens one never pays for it — and so the download happens with
 * a reason on screen for it, instead of as an unexplained stall during the
 * first start-up. [state] is what the browser screen renders while waiting.
 */
object Chromium {

    /** Where the wait is, in the only terms worth showing a reader. */
    sealed interface State {
        /** Nothing asked for yet. */
        data object Idle : State

        /** Fetching or unpacking the bundle. [fraction] is null while unpacking. */
        data class Preparing(val label: String, val fraction: Float?) : State

        /** A browser can be created. */
        data object Ready : State

        /** Give up and offer the system browser instead. */
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    // Its own scope rather than a composition one: the install has to survive
    // the browser screen being closed halfway through it, or reopening a link
    // would restart a download that was nearly finished.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var started = false

    /**
     * Under the user's own data directory, not the working directory. KCEF's
     * default is a relative `kcef-bundle`, which would land wherever the app
     * happened to be launched from — one copy per terminal for a developer,
     * and an unwritable one inside the .app bundle once packaged.
     */
    private val installDir: File
        get() = File(System.getProperty("user.home"), "Library/Application Support/DuskRead/kcef")

    fun ensureStarted() {
        if (started) return
        started = true

        scope.launch {
            KCEF.init(
                builder = {
                    installDir(installDir)
                    progress {
                        onLocating { _state.value = State.Preparing("Looking for Chromium", null) }
                        onDownloading { percent ->
                            _state.value = State.Preparing("Downloading Chromium", percent / 100f)
                        }
                        onExtracting { _state.value = State.Preparing("Unpacking", null) }
                        onInstall { _state.value = State.Preparing("Installing", null) }
                        onInitializing { _state.value = State.Preparing("Starting up", null) }
                        onInitialized { _state.value = State.Ready }
                    }
                    // Its own cache under the install, so a login survives a
                    // restart without leaving anything in the reader's real
                    // browser profile.
                    settings { cachePath = File(installDir, "cache").absolutePath }
                },
                onError = { error ->
                    _state.value = State.Failed(error?.message ?: "Chromium could not be started.")
                },
                onRestartRequired = {
                    _state.value = State.Failed("Chromium was installed. Restart DuskRead to use it.")
                },
            )
        }
    }

    /**
     * Called on the way out of `main`. CEF runs helper processes of its own,
     * and skipping this leaves them alive after the window has closed.
     */
    fun dispose() {
        if (!started) return
        runCatching { KCEF.disposeBlocking() }
    }
}
