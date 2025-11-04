rootProject.name = "it-multiplatform-android-jvm"

pluginManagement {
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        mavenCentral()
        google()
    }
}
