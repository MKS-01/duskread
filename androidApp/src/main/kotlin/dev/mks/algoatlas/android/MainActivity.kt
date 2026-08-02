package dev.mks.algoatlas.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.mks.algoatlas.App
import dev.mks.algoatlas.ui.home.HomeTab
import dev.mks.algoatlas.ui.home.HomeTabRequest
import dev.mks.algoatlas.ui.home.OpenReaderTabExtra

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
        enableEdgeToEdge()
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

    /** Tapping the Reader notification should land on the Reader tab, not whatever tab the app last showed. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(OpenReaderTabExtra, false) == true) {
            HomeTabRequest.request(HomeTab.READER)
        }
    }
}
