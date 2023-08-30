import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    api(projects.core)

    implementation(kotlin("reflect"))
}

registerDokkaArtifactPublication("dokkaTestApi") {
    artifactId = "dokka-test-api"
}
