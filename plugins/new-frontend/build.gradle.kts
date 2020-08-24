import org.jetbrains.registerDokkaArtifactPublication

plugins {
    kotlin("plugin.serialization") version "1.4.0"
}

dependencies {
    implementation(project(":plugins:base"))
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC") // JVM dependency
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.0.0-RC") // JVM dependency
    testImplementation(project(":plugins:base:test-utils"))
}

registerDokkaArtifactPublication("newFrontendPlugin") {
    artifactId = "new-frontend-plugin"
}