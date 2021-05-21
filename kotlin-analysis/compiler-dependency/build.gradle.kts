import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    val kotlin_version: String by project
    api("org.jetbrains.kotlin:kotlin-compiler:$kotlin_version")
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-kotlin-analysis-compiler-$dokka_version.jar")
        archiveClassifier.set("")
        exclude("**/intellij**")
    }
}

registerDokkaArtifactPublication("kotlinAnalysisCompiler") {
    artifactId = "kotlin-analysis-compiler"
    component = Shadow
}
