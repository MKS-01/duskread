package dev.mks.duskread.summary

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.genai.common.GenAiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The on-device summariser, over AICore's summarisation feature.
 *
 * There were two paths here — the free-form Prompt API first, this as the
 * fallback — until the first turned out never to answer: AICore provisions
 * its capabilities as separate *features*, and a Galaxy S25 reports
 * `FEATURE_NOT_FOUND` for the Prompt API while offering the very same Gemini
 * Nano through this one. The cost is that there is no prompt to write, so the
 * register and length are the feature's to decide and it emits only bullets;
 * [parseSummary] turns those into the paragraph the panel draws.
 *
 * Nothing about the article leaves the device, which is why a cloud model —
 * better at this, and one dependency away — is not what sits behind this.
 */
private class MlKitSummariser(context: Context, length: SummaryLength) : Summariser {
    override var state: SummariserState by mutableStateOf(SummariserState.Checking)
        private set

    private val summarization = SummarizationEngine(context, length)

    /**
     * The download runs here, not in whichever panel asked for it: a reader
     * who starts one and closes the panel has not changed their mind, but the
     * panel's scope dies with it and the engine cancels on flow close.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The download in flight, so a second asker joins it instead of starting another. */
    private var download: Job? = null

    override suspend fun refresh() {
        // A download in flight owns [state]; re-probing answers DOWNLOADING
        // with no bytes, replacing a percentage on screen with a blank one.
        if (download?.isActive == true) return

        state = summarization.status()
    }

    /**
     * Joined, not collected: a caller that goes away stops waiting on the
     * download without stopping it, and a second asker joins the same job.
     */
    override suspend fun prepare() {
        val running = download?.takeIf { it.isActive } ?: scope.launch { runDownload() }.also { download = it }
        running.join()
    }

    private suspend fun runDownload() {
        summarization.download().collect { state = it }

        // The feature's own status is the truth, and a stream that ends
        // without a completion event would leave the panel at a percentage.
        download = null
        refresh()
    }

    // The feature truncates oversized input itself; this keeps what is sent
    // close to what the model can actually use.
    override fun summarise(title: String, text: String): Flow<String> = summarization.summarise(truncateWords(text, SummaryWordBudget))

    fun close() {
        scope.cancel()
        download = null
        summarization.close()
    }
}

/**
 * One summariser per length, for as long as the process lives. Each host used
 * to build its own, and closing one took its binding and any download with
 * it; sharing is what makes "it downloads once" true.
 *
 * The held one is closed when the length changes, which is a deliberate act
 * in Settings and the only moment a client configured for the other shape is
 * obsolete. The model is on the device by then, so that costs a rebind, not a
 * second download.
 */
private object Summarisers {
    private var held: Pair<SummaryLength, MlKitSummariser>? = null

    // The application context: this outlives any activity holding a panel.
    fun of(context: Context, length: SummaryLength): MlKitSummariser {
        held?.let { (heldLength, summariser) ->
            if (heldLength == length) return summariser
            summariser.close()
        }

        return MlKitSummariser(context.applicationContext, length).also { held = length to it }
    }
}

/**
 * Why a summary is not available, in words for the person holding the phone.
 * Anything unrecognised keeps its own message rather than being flattened
 * into "something went wrong", which is never true and never helps. Only the
 * codes the summarisation artifact declares are listed.
 */
internal fun describe(failure: Throwable): String {
    val code = (failure as? GenAiException)?.errorCode ?: return failure.message ?: "The model could not be reached."

    return when (code) {
        GenAiException.ErrorCode.NOT_AVAILABLE -> NoModelHere
        GenAiException.ErrorCode.AICORE_INCOMPATIBLE -> "This phone's AI service is too old for on-device summaries."
        GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE -> "A system update is needed before the model can run."
        GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE -> "Not enough free space to download the model."
        GenAiException.ErrorCode.BUSY -> "The model is busy with something else. Try again in a moment."
        GenAiException.ErrorCode.REQUEST_TOO_LARGE -> "This article is too long for the model to take in one piece."
        GenAiException.ErrorCode.REQUEST_TOO_SMALL -> "There is too little text here to summarise."
        GenAiException.ErrorCode.CANCELLED -> "Summary cancelled."
        else -> failure.message ?: "The model could not finish this one."
    }
}

/** Not disposed on leaving: the instance is shared and its binding lives as long as the process. */
@Composable
actual fun rememberSummariser(length: SummaryLength): Summariser {
    val context = LocalContext.current
    val summariser = remember(context, length) { Summarisers.of(context, length) }

    LaunchedEffect(summariser) { summariser.refresh() }

    return summariser
}

/** True on every device the app installs on — `minSdk` is already above ML Kit GenAI's floor. */
actual fun summariesSupported(): Boolean = true

internal const val NoModelHere = "This phone has no on-device model for DuskRead to use."
