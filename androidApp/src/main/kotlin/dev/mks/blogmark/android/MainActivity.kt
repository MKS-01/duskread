package dev.mks.blogmark.android

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
import dev.mks.blogmark.App
import dev.mks.blogmark.links.SharedLinkRequest
import dev.mks.blogmark.ui.home.HomeTab
import dev.mks.blogmark.ui.home.HomeTabRequest
import dev.mks.blogmark.ui.home.OpenReadbackTabExtra

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Two ways in that are not the launcher icon: the Readback notification,
     * which should land on the Readback tab rather than whatever tab the app
     * last showed, and a shared link, which should be saved and then shown.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(OpenReadbackTabExtra, false) == true) {
            HomeTabRequest.request(HomeTab.READBACK)
        }

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { shared ->
                SharedLinkRequest.offer(shared)
                HomeTabRequest.request(HomeTab.SAVED)
            }
        }
    }
}
