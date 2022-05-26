import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://www.jetbrains.com/intellij-repository/snapshots")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://www.myget.org/F/rd-snapshots/maven/")
}

val intellijCore: Configuration by configurations.creating

fun intellijCoreAnalysis() = zipTree(intellijCore.singleFile).matching {
    include("intellij-core.jar")
}

val jpsStandalone: Configuration by configurations.creating

fun jpsModel() = zipTree(jpsStandalone.singleFile).matching {
    include("jps-model.jar")
    include("aalto-xml-*.jar")
}

dependencies {
    val kotlin_plugin_version: String by project
    api("org.jetbrains.kotlin:common:$kotlin_plugin_version")
    api("org.jetbrains.kotlin:idea:$kotlin_plugin_version") {
        isTransitive = false
    }
    api("org.jetbrains.kotlin:core:$kotlin_plugin_version")
    api("org.jetbrains.kotlin:native:$kotlin_plugin_version")

    val idea_version: String by project
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$idea_version")
    implementation(intellijCoreAnalysis())

    jpsStandalone("com.jetbrains.intellij.idea:jps-standalone:$idea_version")
    implementation(jpsModel())
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
