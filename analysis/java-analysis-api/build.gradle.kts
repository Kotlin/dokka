plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

repositories {
    // Override the shared repositories defined in the root settings.gradle.kts
    // These repositories are very specific and are not needed in other projects
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

    maven("https://www.jetbrains.com/intellij-repository/snapshots") {
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    // TODO [beresnev] change to compileOnly?
    val idea_version = "221.5591.52"
    implementation("com.jetbrains.intellij.java:java-psi:$idea_version")
    implementation(projects.core)
}
