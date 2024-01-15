import DokkaBuildSettingsProperties.Companion.dokkaBuildSettingsProperties

/**
 * Gradle Build Cache conventions.
 *
 * See [DokkaBuildSettingsProperties] for properties.
 *
 * Based on https://github.com/JetBrains/kotlin/blob/2675531624d42851af502a993bbefd65ee3e38ef/repo/gradle-settings-conventions/build-cache/src/main/kotlin/build-cache.settings.gradle.kts
 */

val buildSettingsProps = dokkaBuildSettingsProperties

buildCache {
    local {
        isEnabled = buildSettingsProps.buildCacheLocalEnabled.get()
        if (buildSettingsProps.buildCacheLocalDirectory.orNull != null) {
            directory = buildSettingsProps.buildCacheLocalDirectory.get()
        }
    }

    val remoteBuildCacheUrl = buildSettingsProps.buildCacheUrl.orNull
    if (!remoteBuildCacheUrl.isNullOrEmpty()) {
        remote<HttpBuildCache> {
            url = uri(remoteBuildCacheUrl)
            isPush = buildSettingsProps.buildCachePushEnabled.get()

            if (buildSettingsProps.buildCacheUser.isPresent &&
                buildSettingsProps.buildCachePassword.isPresent
            ) {
                credentials.username = buildSettingsProps.buildCacheUser.get()
                credentials.password = buildSettingsProps.buildCachePassword.get()
            }
        }
    }
}
