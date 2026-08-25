package dev.mks.duskread.summary

import android.content.Context
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * AICore's summarisation feature, wrapped as a [Summariser]'s working parts.
 *
 * The model is told "article, N points, English" and nothing else — there is
 * no prompt to write, so the register is the feature's to decide and what
 * comes back is a list every time. N is what [SummaryLength] sets.
 * [parseSummary] turns that into the paragraph the panel draws. See
 * [MlKitSummariser] for why this is the only engine left.
 *
 * Bridged from `ListenableFuture` by hand rather than by pulling in
 * `kotlinx-coroutines-guava`: two small suspending helpers against a
 * dependency that exists for exactly these three call sites.
 */
internal class SummarizationEngine(private val context: Context, private val length: SummaryLength) {
    private var client: Summarizer? = null

    private fun client(): Summarizer = client ?: Summarization.getClient(
        SummarizerOptions.builder(context)
            .setInputType(SummarizerOptions.InputType.ARTICLE)
            // The one dial this feature exposes, and so the one the length
            // setting is built on. Two of the three are used: the middle step
            // is not different enough from either to be worth a third chip.
            .setOutputType(
                when (length) {
                    SummaryLength.Short -> SummarizerOptions.OutputType.ONE_BULLET
                    SummaryLength.Full -> SummarizerOptions.OutputType.THREE_BULLETS
                },
            )
            .setLanguage(SummarizerOptions.Language.ENGLISH)
            // A word count only estimates tokens, so a page that is one
            // enormous paragraph can still overshoot. Truncating beats refusing.
            .setLongInputAutoTruncationEnabled(true)
            .build(),
    ).also { client = it }

    suspend fun status(): SummariserState = runCatching {
        when (client().checkFeatureStatus().await()) {
            FeatureStatus.AVAILABLE -> SummariserState.Ready(modelName())
            FeatureStatus.DOWNLOADABLE -> SummariserState.Downloadable
            FeatureStatus.DOWNLOADING -> SummariserState.Downloading(null)
            else -> SummariserState.Unavailable(NoModelHere)
        }
    }.getOrElse { failure -> SummariserState.Unavailable(describe(failure)) }

    private suspend fun modelName(): String = runCatching { client().baseModelName.await() }.getOrNull()?.takeIf { it.isNotBlank() }?.let { "Gemini Nano · $it" }
        ?: "Gemini Nano"

    /**
     * A flow of states rather than a callback, so the caller that owns
     * [Summariser.state] can collect it. Failure closes the flow having
     * reported why: a failed download is a state to retry from, not an
     * exception the reader needs.
     */
    fun download(): Flow<SummariserState> = callbackFlow {
        val future = client().downloadFeature(
            object : DownloadCallback {
                private var total = 0L

                override fun onDownloadStarted(bytesToDownload: Long) {
                    total = bytesToDownload
                    trySend(SummariserState.Downloading(null))
                }

                override fun onDownloadProgress(totalBytesDownloaded: Long) {
                    trySend(SummariserState.Downloading((totalBytesDownloaded.toFloat() / total).takeIf { total > 0 }?.coerceIn(0f, 1f)))
                }

                override fun onDownloadCompleted() {
                    close()
                }

                override fun onDownloadFailed(e: GenAiException) {
                    trySend(SummariserState.Unavailable(describe(e)))
                    close()
                }
            },
        )

        awaitClose { future.cancel(true) }
    }

    /** One inference, re-emitted as the answer so far — [Summariser.summarise]'s contract. */
    fun summarise(text: String): Flow<String> = callbackFlow {
        val answer = StringBuilder()
        val request = SummarizationRequest.builder(text).build()
        val future: ListenableFuture<*> = client().runInference(
            request,
            StreamingCallback { chunk ->
                answer.append(chunk)
                trySend(answer.toString())
            },
        )

        future.addListener(
            {
                runCatching { future.get() }
                    .onFailure { failure -> close(failure.cause ?: failure) }
                    .onSuccess { close() }
            },
            Executor(Runnable::run),
        )

        awaitClose { future.cancel(true) }
    }

    fun close() {
        runCatching { client?.close() }
        client = null
    }
}

/**
 * A `ListenableFuture` as a suspending call. Runs on the caller's thread —
 * the listener only hands a value back to a suspended coroutine, so another
 * hop buys nothing. Cancellation propagates.
 */
private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addListener(
        {
            runCatching { get() }
                .onSuccess { value -> continuation.resume(value) }
                // Future.get wraps the GenAiException the messages come from.
                .onFailure { failure -> continuation.resumeWithException(failure.cause ?: failure) }
        },
        Executor(Runnable::run),
    )
    continuation.invokeOnCancellation { cancel(true) }
}
