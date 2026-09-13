package dev.mks.duskread.speech

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One article being read aloud, wherever the read was started from. */
data class SpeechNowPlaying(
    /** The article's own URL. Not shown anywhere — it is how a caller tells "is it my article playing" from a title that could coincidentally match another. */
    val key: String,
    val title: String,
    val fraction: Float,
    val playing: Boolean,
)

/**
 * The one place "reading aloud" lives, so it can show up in the same floating transport
 * Readback uses and survive the panel that started it closing.
 */
object SpeechSession {
    data class Request(val key: String, val title: String, val text: String)

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request

    private val _state = MutableStateFlow<SpeechNowPlaying?>(null)
    val state: StateFlow<SpeechNowPlaying?> = _state

    /** Starts reading [text] aloud, replacing whatever was already playing. */
    fun start(key: String, title: String, text: String) {
        _request.value = Request(key, title, text)
    }

    /**
     * Stops the read outright.
     */
    fun stop() {
        _request.value = null
        _state.value = null
    }

    /** Only for the platform driver; nothing else should publish state on its behalf. */
    internal fun publish(playing: SpeechNowPlaying?) {
        _state.value = playing
    }
}

/**
 * Mounted once, for the life of the app — the same place [rememberAudioPlayer] gets
 * called from and for the same reason.
 */
@Composable
expect fun DriveSpeechSession()
