import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

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
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-kotlin-analysis-intellij-$dokka_version.jar")
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

registerDokkaArtifactPublication("kotlinAnalysisIntelliJ") {
    artifactId = "kotlin-analysis-intellij"
    component = Shadow
}
