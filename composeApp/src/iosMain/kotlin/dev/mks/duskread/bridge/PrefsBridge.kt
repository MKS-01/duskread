package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.summary.SwipeDefault

/**
 * Reader preferences, one observation per property.
 *
 * Not folded into a single snapshot type: the properties change independently
 * and a combined object would rebuild — and so re-render — the whole of
 * Settings whenever any one of them moved. Swift's store is the right place to
 * gather them, not this side of the bridge.
 *
 * `voice` is absent on purpose. iOS has no `Speaker` actual, so offering a
 * voice setting here would be offering a control that changes nothing. The
 * summary length is absent for the sharper version of the same reason: there
 * is no such setting on either platform any more, because an article's length
 * decides it — and iOS has no summariser to decide it for in the first place.
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
