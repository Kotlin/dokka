import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
}

registerDokkaArtifactPublication("analysisKotlinApi") {
    artifactId = "analysis-kotlin-api"
}
