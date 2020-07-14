import org.jetbrains.configurePublication

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    id("com.jfrog.bintray")
}

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/snapshots")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://kotlin.bintray.com/kotlin-plugin")
}

val intellijCore: Configuration by configurations.creating

fun intellijCoreAnalysis() = zipTree(intellijCore.singleFile).matching {
    include("intellij-core-analysis.jar")
}

dependencies {
    val kotlin_plugin_version: String by project
    api("org.jetbrains.kotlin:ide-common-ij193:$kotlin_plugin_version")
    api("org.jetbrains.kotlin:kotlin-plugin-ij193:$kotlin_plugin_version") {
        //TODO: parametrize ij version after 1.3.70
        isTransitive = false
    }

    val idea_version: String by project
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$idea_version")
    implementation(intellijCoreAnalysis())

    implementation("org.jetbrains:markdown:0.1.41") {
        because("it's published only on bintray")
    }
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-kotlin-analysis-dependencies-$dokka_version.jar")
        archiveClassifier.set("")

        exclude("colorScheme/**")
        exclude("fileTemplates/**")
        exclude("inspectionDescriptions/**")
        exclude("intentionDescriptions/**")
        exclude("tips/**")
        exclude("messages/**")
        exclude("src/**")
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_builtins")
    }
}

configurePublication("kotlin-analysis-dependencies", useShadow = true)
