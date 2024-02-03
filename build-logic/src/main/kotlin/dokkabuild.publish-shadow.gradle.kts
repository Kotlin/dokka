/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.PublicationName

plugins {
    id("dokkabuild.java")
    id("dokkabuild.publish-base")
    id("com.github.johnrengelman.shadow")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.shadowJar {
    // separate directory because otherwise Gradle complains about multiple tasks writing into the same file
    destinationDirectory = project.layout.buildDirectory.dir("shadowLibs")
    // removes the `-all` classifier from the artifact name
    archiveClassifier = ""
}

publishing.publications.register<MavenPublication>(PublicationName.JVM) {
    // shadow.component call should be after the shadowJar task is configured in a build script,
    // because if not, shadow uses the wrong archiveFile (as we change destinationDirectory and archiveClassifier)
    shadow.component(this)
    artifact(tasks.named("sourcesJar"))
    artifact(tasks.named("javadocJar"))
}
