package dev.mks.duskread

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // `main` is the first moment the Wasm module exists, which is exactly
    // when the boot state in `index.html` has done its job — everything
    // before this point was fetching and instantiating it. Removed rather
    // than faded: `ComposeViewport` takes over the body and appends its
    // canvas, so anything left behind sits under the app for good.
    document.getElementById("boot")?.remove()

    ComposeViewport(document.body!!) {
        App()
    }
}
