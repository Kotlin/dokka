import DokkaBuildSettingsProperties.Companion.BUILD_SCAN_USERNAME_DEFAULT
import DokkaBuildSettingsProperties.Companion.dokkaBuildSettingsProperties

/**
 * Gradle Enterprise conventions.
 *
 * See [DokkaBuildSettingsProperties] for properties.
 *
 * Based on https://github.com/JetBrains/kotlin/blob/19073b96a7ed53dbda61337465ca898c1482e090/repo/gradle-settings-conventions/gradle-enterprise/src/main/kotlin/gradle-enterprise.settings.gradle.kts
 */

plugins {
    id("com.gradle.enterprise")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
}

val buildSettingsProps = dokkaBuildSettingsProperties

val buildScanServer = buildSettingsProps.buildScanServer.orNull

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
                isBuildLogging = true
                isUploadInBackground = true
            }
        } else {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }

        val overriddenName = buildSettingsProps.buildScanUsername.orNull
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> "concealed" }
            username { originalUsername ->
                when {
                    buildSettingsProps.buildingOnTeamCity -> "TeamCity"
                    buildSettingsProps.buildingOnCi -> "CI"
                    overriddenName.isNullOrBlank() -> "concealed"
                    overriddenName == BUILD_SCAN_USERNAME_DEFAULT -> originalUsername
                    else -> overriddenName
                }
            }
        }
    }
}
