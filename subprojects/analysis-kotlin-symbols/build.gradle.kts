import org.jetbrains.DokkaPublicationBuilder
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation("com.jetbrains.intellij.platform:core:213.7172.25")

    listOf(
        "com.jetbrains.intellij.platform:util-rt",
        "com.jetbrains.intellij.platform:util-class-loader",
        "com.jetbrains.intellij.platform:util-text-matching",
        "com.jetbrains.intellij.platform:util",
        "com.jetbrains.intellij.platform:util-base",
        "com.jetbrains.intellij.platform:util-xml-dom",
        "com.jetbrains.intellij.platform:core",
        "com.jetbrains.intellij.platform:core-impl",
        "com.jetbrains.intellij.platform:extensions",
        "com.jetbrains.intellij.java:java-psi",
        "com.jetbrains.intellij.java:java-psi-impl"
    ).forEach {
        implementation("$it:213.7172.25") { isTransitive = false }
    }

    implementation(projects.subprojects.analysisKotlinApi)
    implementation(projects.subprojects.analysisKotlinSymbols.compiler)
    //implementation(projects.subprojects.analysisKotlinSymbols.ide)
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

registerDokkaArtifactPublication("analysisKotlinSymbols") {
    artifactId = "analysis-kotlin-symbols"
    component = DokkaPublicationBuilder.Component.Shadow
}
