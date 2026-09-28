package dev.mks.duskread.speech

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One article being read aloud, wherever the read was started from. */
data class SpeechNowPlaying(
    /** The article's own URL. Not shown anywhere — it is how a caller tells "is it my article playing" from a title that could coincidentally match another. */
    val key: String,
    val title: String,
    val fraction: Float,
    val playing: Boolean,
    /** 1-based place in a listen-through; 0 for a single article. */
    val position: Int = 0,
    val total: Int = 0,
) {
    val inQueue: Boolean get() = total > 1

    /** "2/4 · 43%", or just the percentage outside a queue. */
    val label: String get() {
        val percent = "${(fraction * 100).toInt()}%"
        return if (inQueue) "$position/$total · $percent" else percent
    }
}

/**
 * The one place "reading aloud" lives, so it can show up in the same floating transport
 * Readback uses and survive the panel that started it closing.
 */
object SpeechSession {
    data class Request(
        val key: String,
        val title: String,
        val text: String,
        val position: Int = 0,
        val total: Int = 0,
        /** Moved to by the driver itself at the end of a read, so nobody must start it again. */
        val advanced: Boolean = false,
    )

    // Main: the queue's loads and the drivers' callbacks all land on the same thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var queue: ListenQueue? = null
    private var queueJob: Job? = null

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request

    private val _state = MutableStateFlow<SpeechNowPlaying?>(null)
    val state: StateFlow<SpeechNowPlaying?> = _state

    private val _skips = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** A skip asked for in the app, for a driver that has to hand it to its own service. */
    val skips: SharedFlow<Unit> = _skips

    /** Starts reading [text] aloud, replacing whatever was already playing. */
    fun start(key: String, title: String, text: String) {
        clearQueue()
        _request.value = Request(key, title, text)
    }

    /** Starts a listen-through, replacing whatever was playing. */
    fun listen(next: ListenQueue) {
        clearQueue()
        queue = next
        // Shown before the first text lands, so the tap is answered at once.
        next.entries.firstOrNull()?.let {
            _state.value = SpeechNowPlaying(it.url, it.title, 0f, playing = true, position = 1, total = next.entries.size)
        }
        queueJob = scope.launch {
            val first = next.next(scope)
            if (queue !== next) return@launch
            if (first == null) stop() else _request.value = first
        }
    }

    /**
     * Where a read that ended goes next: the queue's following post, or null to stop.
     * [heard] is false for a skip or a failure, which leaves the post unread.
     */
    suspend fun advance(finishedKey: String, heard: Boolean): Request? {
        val current = queue ?: return null
        if (heard) current.heard(finishedKey)
        val next = current.next(scope)?.copy(advanced = true)
        if (queue !== current) return null
        if (next == null) clearQueue() else _request.value = next
        return next
    }

    /** The transport's next button. */
    fun skip() {
        if (queue != null) _skips.tryEmit(Unit)
    }

    /**
     * Stops the read outright.
     */
    fun stop() {
        clearQueue()
        _request.value = null
        _state.value = null
    }

    /** Only for the platform driver; nothing else should publish state on its behalf. */
    internal fun publish(playing: SpeechNowPlaying?) {
        _state.value = playing
    }

    private fun clearQueue() {
        queueJob?.cancel()
        queueJob = null
        queue = null
    }
}

/**
 * Mounted once, for the life of the app — the same place [rememberAudioPlayer] gets
 * called from and for the same reason.
 */
@Composable
expect fun DriveSpeechSession()
