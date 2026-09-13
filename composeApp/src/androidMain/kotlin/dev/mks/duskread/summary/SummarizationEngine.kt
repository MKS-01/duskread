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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * How much summary an article has earned, from how much article there is.
 *
 * This used to be the reader's to choose, as a pair of chips in Settings.
 * Making it a setting asked the wrong person: nobody knows, before reading
 * it, whether a given post wants a sentence or a paragraph — and whichever
 * chip was set applied to every article afterwards, so a 300-word note and a
 * 4,000-word essay came back the same length. The article knows, and its word
 * count is the one thing we already have when the question is asked.
 *
 * The bands are the whole of the model's range: AICore's summarisation
 * feature is configured with a number of points and offers exactly three.
 * [Standard] is new — the middle step was left unused while this was a chip,
 * on the grounds that it was not different enough from either neighbour to be
 * worth making someone choose it. That objection was about choosing, not
 * about the output, and there is no longer anyone to choose.
 *
 * Android-only on purpose. The thresholds are meaningless without the engine
 * they configure, and `Summariser` is the contract `commonMain` owns; this is
 * the adapter's own business.
 */
internal enum class SummaryDepth(val outputType: Int) {
    Brief(SummarizerOptions.OutputType.ONE_BULLET),
    Standard(SummarizerOptions.OutputType.TWO_BULLETS),
    Full(SummarizerOptions.OutputType.THREE_BULLETS),
    ;

    companion object {
        /**
         * Counted by [wordCount], so this and the send budget agree on what a
         * word is. That budget is well above [FullFrom], so a piece long
         * enough to be truncated is long enough to be [Full] either way and
         * it does not matter that this reads the text after the cut.
         */
        fun of(text: String): SummaryDepth = when (wordCount(text)) {
            in 0..<StandardFrom -> Brief
            in StandardFrom..<FullFrom -> Standard
            else -> Full
        }
    }
}

// A post under this is a note: one point is the whole of it. Above [FullFrom]
// is a long read, where three points is the most the model will give and the
// least the piece deserves. The middle band is the ordinary blog post.
private const val StandardFrom = 500
private const val FullFrom = 1_500

/**
 * AICore's summarisation feature, wrapped as a [Summariser]'s working parts.
 *
 * The model is told "article, N points, English" and nothing else — there is
 * no prompt to write, so the register is the feature's to decide and what
 * comes back is a list every time. N is what [SummaryDepth] reads off the
 * article. [parseSummary] turns that into the paragraph the panel draws. See
 * [MlKitSummariser] for why this is the only engine left.
 *
 * One client per depth, built on first use and kept. The output type is baked
 * into `SummarizerOptions` at construction, so a depth that varies per article
 * cannot share one client — and rebuilding on every summary would rebind to
 * AICore each time, which is latency paid in front of the reader. Three thin
 * clients is the cheaper end of that trade.
 *
 * Bridged from `ListenableFuture` by hand rather than by pulling in
 * `kotlinx-coroutines-guava`: two small suspending helpers against a
 * dependency that exists for exactly these three call sites.
 */
internal class SummarizationEngine(private val context: Context) {
    private val clients = mutableMapOf<SummaryDepth, Summarizer>()

    /**
     * [SummaryDepth.Full] is the default because the callers that do not name
     * a depth — the feature check and the download — are asking about the
     * *feature*, which is one model provisioned once whatever the output type.
     * Any client answers those identically; this one picks the depth most
     * likely to be wanted again afterwards.
     */
    private fun client(depth: SummaryDepth = SummaryDepth.Full): Summarizer = clients.getOrPut(depth) {
        Summarization.getClient(
            SummarizerOptions.builder(context)
                .setInputType(SummarizerOptions.InputType.ARTICLE)
                .setOutputType(depth.outputType)
                .setLanguage(SummarizerOptions.Language.ENGLISH)
                // A word count only estimates tokens, so a page that is one
                // enormous paragraph can still overshoot. Truncating beats refusing.
                .setLongInputAutoTruncationEnabled(true)
                .build(),
        )
    }

    suspend fun status(): SummariserState = runCatchingCancellable {
        when (client().checkFeatureStatus().await()) {
            FeatureStatus.AVAILABLE -> SummariserState.Ready(modelName())
            FeatureStatus.DOWNLOADABLE -> SummariserState.Downloadable
            FeatureStatus.DOWNLOADING -> SummariserState.Downloading(null)
            else -> SummariserState.Unavailable(NoModelHere)
        }
    }.getOrElse { failure -> SummariserState.Unavailable(describe(failure)) }

    private suspend fun modelName(): String = runCatchingCancellable { client().baseModelName.await() }.getOrNull()?.takeIf { it.isNotBlank() }?.let { "Gemini Nano · $it" }
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
        val future: ListenableFuture<*> = client(SummaryDepth.of(text)).runInference(
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
        clients.values.forEach { client -> runCatching { client.close() } }
        clients.clear()
    }
}

/**
 * [runCatching], minus the one thing a suspending function must never catch.
 *
 * `runCatching` catches `CancellationException` along with everything else,
 * so a coroutine that was merely *told to stop* comes back as a failed
 * `Result`. That cost a bug worth naming: a cancellation carries no message,
 * so it reached [describe], fell through to its unknown-failure fallback, and
 * a panel closed while the feature check was still in flight left the shared
 * summariser reading "The model could not be reached." — for the rest of the
 * process, since [Summarisers] holds one for the life of it.
 */
private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    Result.failure(failure)
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
