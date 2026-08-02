import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Since AGP 9 the Android side of a KMP module is configured here rather
    // than with the `com.android.library` plugin.
    androidLibrary {
        namespace = "dev.mks.stacks"
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

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
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
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.haze)
            // The Pomodoro clock's shared state is a StateFlow, read the same
            // way whether a coroutine or a foreground service is driving it.
            implementation(libs.kotlinx.coroutines.core)
            // The dashboard's Trending card: fetches and parses the AI/LLM
            // feed. Ktor picks up whichever engine each platform source set
            // below declares; no expect/actual needed to create the client.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            // Loads the Trending card's thumbnails; ships its own Ktor-backed
            // network fetcher so it reuses the same multiplatform story.
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }

        androidMain.dependencies {
            // Supplies BackHandler for the single-pane navigation.
            implementation(libs.androidx.activity.compose)
            // Chrome Custom Tabs, for opening reference links in-app.
            implementation(libs.androidx.browser)
            // NotificationCompat, for the Pomodoro foreground-service notification.
            implementation(libs.androidx.core)
            // Navigating the SAF tree the Reader folder picker returns.
            implementation(libs.androidx.documentfile)
            // MediaSessionCompat + NotificationCompat.MediaStyle, for proper
            // lock-screen/notification/Bluetooth controls on Reader playback.
            implementation(libs.androidx.media)
            // Ktor's engine for the Trending fetch.
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            // Reads readback's library.db directly; the Reader's only
            // non-Android platform with a real (if manual) way to point at
            // a synced folder.
            implementation(libs.sqlite.jdbc)
            implementation(libs.ktor.client.okhttp)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.mks.stacks.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Stacks"
            packageVersion = "1.0.0"
        }
    }
}
