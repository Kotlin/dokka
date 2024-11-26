rootProject.name = "it-kotlin-multiplatform"

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

        //region Declare Kotlin/Native dependencies - workaround for https://youtrack.jetbrains.com/issue/KT-51379
        // Remove this repo when the only supported KGP version is above 2.0.0
        ivy("https://download.jetbrains.com/kotlin/native/builds") {
            name = "Kotlin Native"
            patternLayout {

                // example download URLs:
                // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/linux-x86_64/kotlin-native-prebuilt-linux-x86_64-1.7.20.tar.gz
                // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/windows-x86_64/kotlin-native-prebuilt-windows-x86_64-1.7.20.zip
                // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/macos-x86_64/kotlin-native-prebuilt-macos-x86_64-1.7.20.tar.gz
                listOf(
                    "macos-x86_64",
                    "macos-aarch64",
                    "osx-x86_64",
                    "osx-aarch64",
                    "linux-x86_64",
                    "windows-x86_64"
                ).forEach { os ->
                    listOf("dev", "releases").forEach { stage ->
                        artifact("$stage/[revision]/$os/[artifact]-[revision].[ext]")
                    }
                }
            }
            metadataSources { artifact() }
            content { includeModuleByRegex(".*", ".*kotlin-native-prebuilt.*") }
        }
        //endregion
    }
}
