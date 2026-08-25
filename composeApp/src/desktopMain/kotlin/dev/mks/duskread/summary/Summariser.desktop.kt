package dev.mks.duskread.summary

import androidx.compose.runtime.Composable

/** No on-device model outside Android — see [UnavailableSummariser]. */
@Composable
actual fun rememberSummariser(length: SummaryLength): Summariser = UnavailableSummariser

actual fun summariesSupported(): Boolean = false
