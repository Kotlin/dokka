/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import org.jetbrains.MAVEN_JVM_PUBLICATION_NAME

plugins {
    id("org.jetbrains.conventions.base-java")
    id("org.jetbrains.conventions.publishing-base")
    id("com.github.johnrengelman.shadow")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing.publications.register<MavenPublication>(MAVEN_JVM_PUBLICATION_NAME) {
    shadow.component(this)
    artifact(tasks["sourcesJar"])
    artifact(tasks["javadocJar"])
}

// There are several reasons for shadowing all dependencies in one place:
// 1. Some of the artifacts Dokka depends on, like com.jetbrains.intellij.java:java-psi, are not
//    published to Maven Central, so the users would need to add custom repositories to their build scripts.
// 2. There are many intertwining transitive dependencies of different versions, as well as direct copy-paste,
//    that can lead to runtime errors due to classpath conflicts, so it's best to let Gradle take care of
//    dependency resolution, and then pack everything into a single jar in a single place that can be tuned.
// 3. The compiler and ide modules are internal details that are likely to change, so packing everything into
//    a single jar provides some stability for the CLI users, while not exposing too many internals. Publishing
//    the compiler, ide and other subprojects separately would make it difficult to refactor the project structure.
tasks.shadowJar {
    // removes `-all` classifier from artifact name, so that it replaces original one
    archiveClassifier.set("")
    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}
