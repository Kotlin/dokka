import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)

    implementation(libs.jsoup)
    implementation(libs.jetbrains.markdown)
}

registerDokkaArtifactPublication("analysis-markdown")
