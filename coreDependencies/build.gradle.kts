plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}

val intellijCore: Configuration by configurations.creating

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/snapshots")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://kotlin.bintray.com/kotlin-plugin")
}

fun intellijCoreAnalysis() = zipTree(intellijCore.singleFile).matching {
    include("intellij-core-analysis.jar")
}

dependencies {
    val idea_version: String by project
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$idea_version")
    val kotlin_plugin_version: String by project
    implementation(intellijCoreAnalysis())
    implementation("org.jetbrains.kotlin:kotlin-plugin-ij193:$kotlin_plugin_version") {
        //TODO: parametrize ij version after 1.3.70
        isTransitive = false
    }
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-dependencies-$dokka_version.jar")
        archiveClassifier.set("")

        exclude("colorScheme/**")
        exclude("fileTemplates/**")
        exclude("inspectionDescriptions/**")
        exclude("intentionDescriptions/**")
        exclude("tips/**")
        exclude("messages/**")
        exclude("src/**")
    }
}
