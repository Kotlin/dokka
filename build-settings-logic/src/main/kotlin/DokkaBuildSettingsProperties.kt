/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import javax.inject.Inject

/**
 * Shared properties in `settings.gradle.kts` files.
 *
 * Fetch an instance using [DokkaBuildSettingsProperties.Companion.dokkaBuildSettingsProperties].
 */
abstract class DokkaBuildSettingsProperties @Inject constructor(
    private val providers: ProviderFactory
) {
    val buildingOnTeamCity: Boolean = System.getenv("TEAMCITY_VERSION") != null
    val buildingOnGitHub: Boolean = System.getenv("GITHUB_ACTION") != null
    val buildingOnCi: Boolean = System.getenv("CI") != null || buildingOnTeamCity || buildingOnGitHub


    //region Gradle Build Scan
    // NOTE: build scan properties are documented in CONTRIBUTING.md
    val buildScanEnabled: Provider<Boolean> =
        dokkaProperty("build.scan.enabled", String::toBoolean)
            .orElse(buildingOnCi)

    /** Optionally override the default name attached to a Build Scan. */
    val buildScanUsername: Provider<String> =
        dokkaProperty("build.scan.username")
            .orElse(BUILD_SCAN_USERNAME_DEFAULT)
            .map(String::trim)
    //endregion


    private fun dokkaProperty(name: String): Provider<String> =
        providers.gradleProperty("org.jetbrains.dokka.$name")

    private fun <T : Any> dokkaProperty(name: String, convert: (String) -> T): Provider<T> =
        dokkaProperty(name).map(convert)

    companion object {
        const val BUILD_SCAN_USERNAME_DEFAULT = "<default>"

        val Settings.dokkaBuildSettingsProperties: DokkaBuildSettingsProperties
            get() {
                return extensions.findByType<DokkaBuildSettingsProperties>()
                    ?: extensions.create<DokkaBuildSettingsProperties>("dokkaBuildSettingsProperties")
            }
    }
}
