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
 * The one place "reading aloud" lives, so it can show up in the same floating
 * transport Readback uses and survive the panel that started it closing.
 *
 * A thin request/state pair, the same shape `SummaryRequest` already proved
 * for handing work from a swiped row to something mounted once at the root.
 * [request] is "please read this"; the coroutine that actually drives a
 * [Speaker] through it lives in `HomeScreen` — the one place already alive
 * for the life of the app, the same place [dev.mks.duskread.reader.AudioPlayer]
 * lives for exactly this reason. [state] is what that coroutine reports back,
 * which is what the floating bar and whichever panel started the read both
 * watch to draw themselves.
 *
 * A single session, not one per caller: starting a new read replaces
 * whatever was already playing, the same as pressing play on a different
 * Readback item does today. There is one floating transport, so there can
 * only ever be one thing in it.
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
     *
     * There is no pause that resumes from where it left off — the platform
     * engine this runs on on has none either, see `SystemSpeaker.pause` — so
     * this is the one control a session offers besides starting a new one.
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
 * Mounted once, for the life of the app — the same place [rememberAudioPlayer]
 * gets called from and for the same reason: whatever actually turns a
 * [SpeechSession] request into sound has to outlive the panel that made it.
 *
 * Not a value-returning `rememberX` the way [rememberSpeaker] is, because
 * nothing needs to hold onto what this returns — the whole point of it is the
 * side effect of watching [SpeechSession.request] and acting on it. On
 * Android that means a foreground service with a real notification, the same
 * shape `ReaderPlaybackService` already uses and for the same reason: a read
 * that stops the moment the app is backgrounded, with no notification saying
 * it was ever happening, is not "reading aloud" so much as "reading aloud
 * until you switch apps", and Android's own background-execution limits will
 * kill unfinished work with no service holding it up regardless. Every other
 * platform has no engine to drive in the first place — see
 * [speechSupported] — so there this does nothing.
 */
@Composable
expect fun DriveSpeechSession()
