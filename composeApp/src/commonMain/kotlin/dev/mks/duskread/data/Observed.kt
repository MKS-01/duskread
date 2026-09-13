package dev.mks.duskread.data

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty

/**
 * A property that is both Compose snapshot state and a [StateFlow].
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
