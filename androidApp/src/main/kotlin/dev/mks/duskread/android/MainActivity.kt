package dev.mks.duskread.android

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.mks.duskread.App
import dev.mks.duskread.android.widget.DuskReadWidget
import dev.mks.duskread.links.LinkInbox
import dev.mks.duskread.links.SharedLinkRequest
import dev.mks.duskread.ui.home.HomeTab
import dev.mks.duskread.ui.home.HomeTabRequest
import dev.mks.duskread.ui.home.OpenReadbackTabExtra
import dev.mks.duskread.ui.pomodoro.FocusRequest
import dev.mks.duskread.ui.pomodoro.OpenFocusExtra

class MainActivity : ComponentActivity() {
    // Requested eagerly at launch rather than deferred to the first Pomodoro
    // start, so the timer's notification doesn't need a permission callback
    // threaded down through Compose for one rarely-changing decision.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the splash window is in place for
        // the very first frame. It is dismissed automatically once there is
        // content to draw — nothing here holds it open.
        installSplashScreen()
        // Pinned dark rather than left on the default `auto`, which follows the
        // system's light/dark setting: both app themes are dark, so on a phone
        // set to light mode `auto` would draw dark status-bar icons over our
        // near-black background and lose them entirely.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleIntent(intent)
        setContent { App() }
    }

    /**
     * Anything the home-screen widget captured while we were away is filed
     * now. A cold start would have drained it as HomeScreen first composed,
     * but a warm one composes nothing — the Compose tree is still there and
     * has no idea the reader has been elsewhere — so the poke is what makes
     * a capture show up in Saved on the very next visit rather than the one
     * after that.
     */
    override fun onResume() {
        super.onResume()
        LinkInbox.poke()
    }

    /**
     * The widget follows the app's Ink / Paper Black toggle, and there is no
     * change broadcast on the preference it reads. Repainting as the app
     * leaves the foreground catches every toggle, once, at the only moment
     * the reader could be looking at the widget instead. It also reconciles
     * a focus session that ended while the process was gone.
     */
    override fun onStop() {
        super.onStop()
        DuskReadWidget.refresh(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Three ways in that are not the launcher icon: the Readback
     * notification, which should land on the Readback tab rather than
     * whatever tab the app last showed; a shared link, which should be saved
     * and then shown; and the home-screen widget's running-session cell,
     * which should land on the timer.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(OpenReadbackTabExtra, false) == true) {
            HomeTabRequest.request(HomeTab.READBACK)
        }

        if (intent?.getBooleanExtra(OpenFocusExtra, false) == true) {
            FocusRequest.request()
        }

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { shared ->
                SharedLinkRequest.offer(shared)
                HomeTabRequest.request(HomeTab.SAVED)
            }
        }
    }
}
