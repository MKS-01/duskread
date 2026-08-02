package dev.mks.stacks.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/** `javax.sound.sampled.Clip` — built into the JDK, and a WAV is exactly what it's for. */
internal class DesktopAudioPlayer(private val repository: DesktopReadRepository, private val scope: CoroutineScope) : AudioPlayer {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state

    private var clip: Clip? = null
    private var progressJob: Job? = null

    override fun play(item: ReadItem) {
        release()
        val file = repository.audioFile(item)?.takeIf { it.exists() } ?: return

        val newClip = AudioSystem.getClip()
        AudioSystem.getAudioInputStream(file).use { newClip.open(it) }
        newClip.start()
        clip = newClip
        _state.value = PlaybackState(item = item, playing = true, durationSec = newClip.microsecondLength / 1_000_000f)
        startProgressLoop()
    }

    override fun togglePlayPause() {
        val current = clip ?: return
        if (current.isRunning) {
            current.stop()
            progressJob?.cancel()
            _state.value = _state.value.copy(playing = false)
        } else {
            current.start()
            _state.value = _state.value.copy(playing = true)
            startProgressLoop()
        }
    }

    override fun seekTo(seconds: Float) {
        clip?.microsecondPosition = (seconds * 1_000_000).toLong()
        _state.value = _state.value.copy(positionSec = seconds)
    }

    override fun stop() = release()

    private fun release() {
        progressJob?.cancel()
        clip?.close()
        clip = null
        _state.value = PlaybackState()
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                delay(300)
                val current = clip ?: break
                _state.value = _state.value.copy(positionSec = current.microsecondPosition / 1_000_000f)
                if (!current.isRunning) {
                    _state.value = _state.value.copy(playing = false)
                    break
                }
            }
        }
    }
}

@Composable
actual fun rememberAudioPlayer(repository: ReadRepository): AudioPlayer {
    val scope = rememberCoroutineScope()
    val desktopRepository = repository as DesktopReadRepository
    return remember(repository) { DesktopAudioPlayer(desktopRepository, scope) }
}
