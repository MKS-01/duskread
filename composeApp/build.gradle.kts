import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Since AGP 9 the Android side of a KMP module is configured here rather
    // than with the `com.android.library` plugin.
    androidLibrary {
        namespace = "dev.mks.duskread"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }

        // Only for the Custom Tab transition animations, which have to be
        // platform anim resources rather than Compose animations.
        androidResources { enable = true }
    }

    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // iosX64 (Intel simulator) dropped: Compose Multiplatform 1.11 no longer
    // publishes runtime/foundation/ui for it, matching Apple's own removal of
    // Intel simulator support.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.haze)
            // The Pomodoro clock's shared state is a StateFlow, read the same
            // way whether a coroutine or a foreground service is driving it.
            implementation(libs.kotlinx.coroutines.core)
            // Saved links fetch the page title of a pasted URL. Only the core
            // is shared — every target brings its own engine below, since
            // there is no engine that works on all five.
            implementation(libs.ktor.client.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // Supplies BackHandler for the single-pane navigation.
            implementation(libs.androidx.activity.compose)
            // WebSettingsCompat.setAlgorithmicDarkeningAllowed for the embedded,
            // force-darkened reader WebView that opens reference links in-app.
            implementation(libs.androidx.webkit)
            // NotificationCompat, for the Pomodoro foreground-service notification.
            implementation(libs.androidx.core)
            // Navigating the SAF tree the Reader folder picker returns.
            implementation(libs.androidx.documentfile)
            // MediaSessionCompat + NotificationCompat.MediaStyle, for proper
            // lock-screen/notification/Bluetooth controls on Reader playback.
            implementation(libs.androidx.media)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            // Reads readback's library.db directly; the Reader's only
            // non-Android platform with a real (if manual) way to point at
            // a synced folder.
            implementation(libs.sqlite.jdbc)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.mks.duskread.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "DuskRead"
            packageVersion = "1.0.0"
        }
    }
}
