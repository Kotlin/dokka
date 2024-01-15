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
    val buildingOnCi: Boolean = System.getenv("CI") != null || buildingOnTeamCity


    //region Gradle Build Scan
    val buildScanUrl: Provider<String> =
        dokkaProperty("build.scan.url")
    val buildScanUsername: Provider<String> =
        dokkaProperty("build.scan.username").map(String::trim)
    //endregion


    //region Gradle Build Cache
    val buildCacheLocalEnabled: Provider<Boolean> =
        dokkaProperty("build.cache.local.enabled", String::toBoolean)
            .orElse(!buildingOnCi)
            .orElse(true)
    val buildCacheLocalDirectory: Provider<String> =
        dokkaProperty("build.cache.local.directory")
    val buildCacheUrl: Provider<String> =
        dokkaProperty("build.cache.url").map(String::trim)
    val buildCachePushEnabled: Provider<Boolean> =
        dokkaProperty("build.cache.push", String::toBoolean)
            .orElse(buildingOnTeamCity)
            .orElse(false)
    val buildCacheUser: Provider<String> =
        dokkaProperty("build.cache.user")
    val buildCachePassword: Provider<String> =
        dokkaProperty("build.cache.password")
    //endregion


    private fun dokkaProperty(name: String): Provider<String> =
        providers.gradleProperty("org.jetbrains.dokka.$name")

    private fun <T : Any> dokkaProperty(name: String, convert: (String) -> T) =
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
