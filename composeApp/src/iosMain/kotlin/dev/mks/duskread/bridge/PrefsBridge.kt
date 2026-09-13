package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.summary.SwipeDefault

/**
 * Reader preferences, one observation per property.
 */
class PrefsBridge internal constructor(private val graph: AppGraph) {
    private val prefs get() = graph.prefs

    fun observeName(onEach: (String?) -> Unit): Cancellable = prefs.nameUpdates.watch(onEach)

    fun observeIntroSeen(onEach: (Boolean) -> Unit): Cancellable = prefs.introSeenUpdates.watch(onEach)

    fun observeMono(onEach: (Boolean) -> Unit): Cancellable = prefs.monoUpdates.watch(onEach)

    fun observeSwipeDefault(onEach: (SwipeDefault) -> Unit): Cancellable = prefs.swipeDefaultUpdates.watch(onEach)

    fun name(): String? = prefs.name

    fun introSeen(): Boolean = prefs.introSeen

    fun mono(): Boolean = prefs.mono

    fun updateName(value: String?) = prefs.updateName(value)

    fun markIntroSeen() = prefs.markIntroSeen()

    fun updateMono(value: Boolean) = prefs.updateMono(value)

    fun updateSwipeDefault(value: SwipeDefault) = prefs.updateSwipeDefault(value)

    fun reset() = prefs.reset()
}
