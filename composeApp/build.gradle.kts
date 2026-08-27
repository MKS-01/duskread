import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

/**
 * Writes `AppVersion` into commonMain from the `app` entry in the version
 * catalog — the same entry androidApp uses for `versionName`.
 *
 * A generated file rather than a checked-in constant because the value has
 * two consumers in two languages: Gradle cannot read a Kotlin `const`, so the
 * only way to keep one source of truth is for the build to own it.
 */
val generateAppVersion by tasks.registering {
    val version = libs.versions.app.get()
    val outputDir = layout.buildDirectory.dir("generated/appVersion")

    inputs.property("version", version)
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().file("dev/mks/duskread/AppVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package dev.mks.duskread

            /** Generated from `app` in gradle/libs.versions.toml. Do not edit. */
            const val AppVersion: String = "$version"
            """.trimIndent() + "\n",
        )
    }
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
        // The version string, written into commonMain rather than declared
        // there. Settings has to show it and the Android manifest has to
        // report it, and two hand-kept copies of a number are two chances for
        // the About line to start lying about which build you are holding.
        commonMain { kotlin.srcDir(generateAppVersion) }

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
            // The on-device summariser talks to AICore through this. Android
            // only — every other target reports summaries as unavailable, so
            // there is nothing to add to their source sets.
            implementation(libs.mlkit.genai.summarization)
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
            // Embedded Chromium, so a saved link opens in the app rather than
            // handing the reader off to Safari. Android has a WebView in the
            // platform; the JVM has nothing, so the browser has to be brought
            // along. See `InAppBrowserScreen.desktop.kt`.
            implementation(libs.kcef)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.mks.duskread.MainKt"

        // JCEF reaches into AWT's macOS internals to hand Chromium a native
        // surface, and the module system closed those packages in JDK 17.
        // Without these the browser fails at `createBrowser`, not at start-up,
        // so the app looks fine until the first link is opened.
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "DuskRead"
            packageVersion = "1.0.0"

            // jpackage builds a cut-down runtime, and by default it holds
            // none of these. What each is for, since a missing one only
            // shows up as a ClassNotFoundException in the packaged app and
            // never in `run`: `java.sql` is sqlite-jdbc reading readback's
            // library.db, `jdk.unsupported` is sun.misc.Unsafe underneath
            // both Skiko and CEF, and the other two are what JCEF's own
            // start-up touches. From `./gradlew :composeApp:suggestRuntimeModules`.
            modules("java.instrument", "java.management", "java.sql", "jdk.unsupported")
        }
    }
}
