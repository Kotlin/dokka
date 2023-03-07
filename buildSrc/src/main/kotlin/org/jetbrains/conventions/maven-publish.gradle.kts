package org.jetbrains.conventions

plugins {
    id("org.jetbrains.conventions.base")
    `maven-publish`
    signing
    id("org.jetbrains.conventions.dokka")
}

val javadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles a Javadoc JAR using Dokka HTML"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    repositories {
        // Publish to a project-local Maven directory, for verification. To test, run:
        // ./gradlew publishAllPublicationsToMavenProjectLocalRepository
        // and check $rootDir/build/maven-project-local
        maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
            name = "MavenProjectLocal"
        }
    }

    publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar)
    }
}
