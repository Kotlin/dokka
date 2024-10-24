import com.android.build.api.dsl.SettingsExtension

pluginManagement {
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        mavenCentral()
        google()
    }
}

plugins {
//    id("com.android.settings") /* %{AGP_VERSION} */
    id("com.android.settings") version "8.1.1"
}

rootProject.name = "ComposeMenuProvider"

include(
    ":core",
    ":material3",
)

configure<SettingsExtension> {
    buildToolsVersion = "34.0.0"
    compileSdk = 34
    minSdk = 28
}
