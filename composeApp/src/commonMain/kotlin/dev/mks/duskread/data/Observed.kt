package dev.mks.duskread.data

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty

/**
 * A property that is both Compose snapshot state and a [StateFlow].
 *
 * Every state holder in the app stores its state in `mutableStateOf`, which is
 * exactly right for Compose and completely invisible to anything else: from
 * Swift a snapshot-backed property compiles to a plain getter with no change
 * notification at all. The SwiftUI iOS shell needs something it can subscribe
 * to.
 *
 * Migrating the holders to `StateFlow` outright would mean a `collectAsState()`
 * at roughly a hundred and fifty Compose read sites in the app that is built and
 * used daily, with no tests to catch the one that was missed. So both
 * representations are kept and a single setter writes both — they cannot drift,
 * because there is no way to write one without the other.
 *
 * Reads go to the snapshot rather than the flow so Compose still subscribes the
 * calling composition; [updates] is for everyone else.
 */
class Observed<T>(initial: T) {
    private val snapshot = mutableStateOf(initial)
    private val flow = MutableStateFlow(initial)

    /** For observers outside a composition — currently the iOS bridge. */
    val updates: StateFlow<T> get() = flow

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = snapshot.value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        snapshot.value = value
        flow.value = value
    }
}
