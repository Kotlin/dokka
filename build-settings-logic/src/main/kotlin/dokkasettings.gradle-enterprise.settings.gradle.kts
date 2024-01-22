/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import DokkaBuildSettingsProperties.Companion.BUILD_SCAN_USERNAME_DEFAULT
import DokkaBuildSettingsProperties.Companion.dokkaBuildSettingsProperties

/**
 * Gradle Enterprise conventions.
 *
 * See [DokkaBuildSettingsProperties] for properties.
 *
 * To use JetBrain's Gradle Enterprise set the URL
 * https://ge.jetbrains.com/
 * in `$GRADLE_USER_HOME/gradle.properties`†
 *
 * ```properties
 * org.jetbrains.dokka.build.scan.url=https\://ge.jetbrains.com/
 * ```
 *
 * Based on https://github.com/JetBrains/kotlin/blob/19073b96a7ed53dbda61337465ca898c1482e090/repo/gradle-settings-conventions/gradle-enterprise/src/main/kotlin/gradle-enterprise.settings.gradle.kts
 *
 * † _See [`GRADLE_USER_HOME`](https://docs.gradle.org/8.5/userguide/directory_layout.html#dir:gradle_user_home)_
 */

plugins {
    id("com.gradle.enterprise")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
}

val buildSettingsProps = dokkaBuildSettingsProperties

val buildScanServer = buildSettingsProps.buildScanUrl.orNull?.ifBlank { null }

if (buildScanServer != null) {
    plugins.apply("com.gradle.common-custom-user-data-gradle-plugin")
}

gradleEnterprise {
    buildScan {
        if (buildScanServer != null) {
            server = buildScanServer
            publishAlways()

            capture {
                isTaskInputFiles = true
                isBuildLogging = true
                isUploadInBackground = true
            }
        }

        val overriddenName = buildSettingsProps.buildScanUsername.orNull
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> "concealed" }
            username { originalUsername ->
                when {
                    buildSettingsProps.buildingOnTeamCity -> "TeamCity"
                    buildSettingsProps.buildingOnCi -> "CI"
                    overriddenName.isNullOrBlank() -> overriddenName
                    overriddenName == BUILD_SCAN_USERNAME_DEFAULT -> originalUsername
                    else -> "unknown"
                }
            }
        }
    }
}
