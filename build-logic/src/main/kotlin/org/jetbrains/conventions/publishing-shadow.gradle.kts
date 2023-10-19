/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import org.gradle.jvm.tasks.Jar
import org.jetbrains.MAVEN_PUBLICATION_NAME

plugins {
    id("org.jetbrains.conventions.publishing-base")
    id("com.github.johnrengelman.shadow")
}

val javadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    // TODO: decide if we really need dokka html in javadoc jar
//    description = "Assembles a Javadoc JAR using Dokka HTML"
//    from(tasks.dokkaHtml)
}

publishing.publications.register<MavenPublication>(MAVEN_PUBLICATION_NAME) {
    artifact(javadocJar)
    shadow.component(this)
    artifact(tasks["sourcesJar"])
}

// Manually disable publication of Shadow elements https://github.com/johnrengelman/shadow/issues/651#issue-839148311
// This is done to preserve compatibility and have the same behavior as previous versions of Dokka.
// For more details, see https://github.com/Kotlin/dokka/pull/2704#issuecomment-1499517930
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }

// TODO: recheck, may be it's not needed as we remove shadowRuntimeElements?
// TODO: configure it here
//tasks {
//    // There are several reasons for shadowing all dependencies in one place:
//    // 1. Some of the artifacts Dokka depends on, like com.jetbrains.intellij.java:java-psi, are not
//    //    published to Maven Central, so the users would need to add custom repositories to their build scripts.
//    // 2. There are many intertwining transitive dependencies of different versions, as well as direct copy-paste,
//    //    that can lead to runtime errors due to classpath conflicts, so it's best to let Gradle take care of
//    //    dependency resolution, and then pack everything into a single jar in a single place that can be tuned.
//    // 3. The compiler and ide modules are internal details that are likely to change, so packing everything into
//    //    a single jar provides some stability for the CLI users, while not exposing too many internals. Publishing
//    //    the compiler, ide and other subprojects separately would make it difficult to refactor the project structure.
//    shadowJar {
//        val dokka_version: String by project
//
//        // cannot be named exactly like the artifact (i.e analysis-kotlin-descriptors-VER.jar),
//        // otherwise leads to obscure test failures when run via CLI, but not via IJ
//        archiveFileName.set("analysis-kotlin-descriptors-all-$dokka_version.jar")
//        archiveClassifier.set("")
//
//        // service files are merged to make sure all Dokka plugins
//        // from the dependencies are loaded, and not just a single one.
//        mergeServiceFiles()
//    }
//}
