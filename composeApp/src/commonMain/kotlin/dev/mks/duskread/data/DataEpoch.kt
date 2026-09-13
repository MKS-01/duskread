package dev.mks.duskread.data

/**
 * Which generation of the reader's data is the current one. It exists for exactly one
 * race, and that race is the erase.
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
