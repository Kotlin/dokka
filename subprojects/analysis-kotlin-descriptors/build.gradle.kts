/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.DokkaPublicationBuilder
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    // to override some interfaces (JvmAnnotationEnumFieldValue, JvmAnnotationConstantValue) from compiler since thet are empty there
    // should be `api` since we already have it in :analysis-java-psi
    api(libs.intellij.java.psi.api) {
        isTransitive = false
    }
    implementation(projects.subprojects.analysisKotlinApi)
    implementation(projects.subprojects.analysisKotlinDescriptors.compiler)
    implementation(projects.subprojects.analysisKotlinDescriptors.ide)
}

tasks {
    // There are several reasons for shadowing all dependencies in one place:
    // 1. Some of the artifacts Dokka depends on, like com.jetbrains.intellij.java:java-psi, are not
    //    published to Maven Central, so the users would need to add custom repositories to their build scripts.
    // 2. There are many intertwining transitive dependencies of different versions, as well as direct copy-paste,
    //    that can lead to runtime errors due to classpath conflicts, so it's best to let Gradle take care of
    //    dependency resolution, and then pack everything into a single jar in a single place that can be tuned.
    // 3. The compiler and ide modules are internal details that are likely to change, so packing everything into
    //    a single jar provides some stability for the CLI users, while not exposing too many internals. Publishing
    //    the compiler, ide and other subprojects separately would make it difficult to refactor the project structure.
    shadowJar {
        val dokka_version: String by project

        // cannot be named exactly like the artifact (i.e analysis-kotlin-descriptors-VER.jar),
        // otherwise leads to obscure test failures when run via CLI, but not via IJ
        archiveFileName.set("analysis-kotlin-descriptors-all-$dokka_version.jar")
        archiveClassifier.set("")

        // service files are merged to make sure all Dokka plugins
        // from the dependencies are loaded, and not just a single one.
        mergeServiceFiles()
    }
}

registerDokkaArtifactPublication("analysisKotlinDescriptors") {
    artifactId = "analysis-kotlin-descriptors"
    component = DokkaPublicationBuilder.Component.Shadow
}
