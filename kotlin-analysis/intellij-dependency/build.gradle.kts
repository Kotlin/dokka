import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}

repositories {
    mavenCentral()

    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
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
    implementation("com.jetbrains.intellij.java:java-psi-impl:$idea_version")
    implementation("com.jetbrains.intellij.platform:util-rt:$idea_version")
    implementation("com.jetbrains.intellij.platform:jps-model-impl:$idea_version")
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
