import org.jetbrains.DokkaPublicationBuilder
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(projects.subprojects.analysisKotlinApi)
    implementation(projects.subprojects.analysisKotlinSymbols.compiler)
    implementation(projects.subprojects.analysisKotlinSymbols.ide)
}

tasks {
    shadowJar {
        val dokka_version: String by project

        // cannot be named exactly like the artifact (i.e analysis-kotlin-symbols-VER.jar),
        // otherwise leads to obscure test failures when run via CLI, but not via IJ
        archiveFileName.set("analysis-kotlin-symbols-all-$dokka_version.jar")
        archiveClassifier.set("")

        // service files are merged to make sure all Dokka plugins
        // from the dependencies are loaded, and not just a single one.
        mergeServiceFiles()
    }
}

registerDokkaArtifactPublication("analysis-kotlin-symbols", DokkaPublicationBuilder.Component.Shadow)
