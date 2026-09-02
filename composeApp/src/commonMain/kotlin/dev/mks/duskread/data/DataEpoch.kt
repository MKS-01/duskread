package dev.mks.duskread.data

/**
 * Which generation of the reader's data is the current one.
 *
 * It exists for exactly one race, and that race is the erase. A sync is a
 * long sequence of network calls with writes in between — `applySources`
 * resolves and follows one blog at a time, `syncFeeds` fetches a dozen feeds
 * before it commits — while the erase in Settings is a handful of synchronous
 * `clear()` calls that finish inside a single tap. Land the tap in the middle
 * of a sync and everything the sync writes afterwards is data the reader has
 * just asked to be rid of, restored from Notion by a coroutine that has no
 * idea it happened. That is what put a full Following list back on Home
 * seconds after "Erase everything".
 *
 * Cancelling the coroutine is not enough on its own. Both syncs are launched
 * from scopes that outlive the tap — Settings fades out before it is disposed,
 * Home's automatic run is only cancelled when the whole screen is torn down —
 * and cancellation is observed at the next suspension point in any case, which
 * is after the write that follows the one already in flight.
 *
 * So the erase bumps the epoch, and anything holding a stale [mark] declines
 * to write. A number rather than a `Job` to cancel, because the question a
 * sync needs answered is not "was I cancelled" but "is what I fetched still
 * about the data that exists" — and that stays answerable however the sync
 * was started, from whichever scope.
 *
 * Not thread-safe, and does not need to be: every reader and the only writer
 * are on the main dispatcher.
 */
object DataEpoch {
    private var epoch = 0

    /** Taken at the start of a sync, handed back to [stale] before each write. */
    fun mark(): Int = epoch

    /** Called first thing by the erase, before anything is actually cleared. */
    fun bump() {
        epoch++
    }

    fun stale(mark: Int): Boolean = mark != epoch
}
