package dev.mks.duskread.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * `remember(keys) { compute() }`, but only the first value is built inline — so the list
 * draws full on its first frame — and every later one on Default, so a sync landing mid-scroll
 * does not stall it.
 */
@Composable
fun <T> rememberOffMain(vararg keys: Any?, compute: () -> T): T {
    val latest = rememberUpdatedState(compute)
    val state = remember { mutableStateOf(compute()) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(*keys) {
        // The inline value above already answers the first set of keys.
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        state.value = withContext(Dispatchers.Default) { latest.value() }
    }
    return state.value
}
