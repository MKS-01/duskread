plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint) apply false
}

// ktlint is applied to every module from here so new modules get it for free.
// Rules that fight the house style are switched off in .editorconfig, not here.
// Read outside the `subprojects` block — the version catalog accessor is only
// visible in the root script's own scope.
val ktlintVersion = libs.versions.ktlint.get()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintVersion)
        // The build already fails loudly on real errors; keep lint out of the
        // normal compile loop so `installDebug` stays fast.
        ignoreFailures.set(false)
        filter {
            exclude { it.file.path.contains("/build/") }
        }
    }
}
