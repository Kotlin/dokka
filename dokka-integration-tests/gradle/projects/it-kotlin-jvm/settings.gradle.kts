rootProject.name = "it-kotlin-jvm"

pluginManagement {
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */

        mavenCentral()
    }
}
