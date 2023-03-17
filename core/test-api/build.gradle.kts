import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    api(projects.core)
    implementation(projects.kotlinAnalysis)
    implementation("junit:junit:4.13.2") // TODO: remove dependency to junit
    implementation(kotlin("reflect"))
}

registerDokkaArtifactPublication("dokkaTestApi") {
    artifactId = "dokka-test-api"
}
