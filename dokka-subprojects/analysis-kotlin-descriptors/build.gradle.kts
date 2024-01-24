/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-shadow")
}

overridePublicationArtifactId("analysis-kotlin-descriptors")

dependencies {
    // to override some interfaces (JvmAnnotationEnumFieldValue, JvmAnnotationConstantValue) from compiler since thet are empty there
    // should be `api` since we already have it in :analysis-java-psi
    api(libs.intellij.java.psi.api) {
        isTransitive = false
    }
    implementation(projects.dokkaSubprojects.analysisKotlinApi)
    implementation(projects.dokkaSubprojects.analysisKotlinDescriptorsCompiler)
    implementation(projects.dokkaSubprojects.analysisKotlinDescriptorsIde)
}

tasks.shadowJar {
    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}

/**
 * hack for shadow jar and fastutil because of kotlin-compiler
 *
 * KT issue: https://youtrack.jetbrains.com/issue/KT-47150
 *
 * what is happening here?
 *   fastutil is removed from shadow-jar completely,
 *   instead we declare a maven RUNTIME dependency on fastutil;
 *   this dependency will be fetched by Gradle at build time (as any other dependency from maven-central)
 *
 * why do we need this?
 *   because `kotlin-compiler` artifact includes unshaded (not-relocated) STRIPPED `fastutil` dependency,
 *   STRIPPED here means, that it doesn't provide full `fastutil` classpath, but only a part of it which is used
 *   and so when shadowJar task is executed it takes classes from `fastutil` from `kotlin-compiler` and adds it to shadow-jar
 *   then adds all other classes from `fastutil` coming from `markdown-jb`,
 *   but because `fastutil` from `kotlin-compiler` is STRIPPED, some classes (like `IntStack`) has no some methods
 *   and so such classes are not replaced afterward by `shadowJar` task - it visits every class once
 *
 */
dependencies.shadow(libs.fastutil)
tasks.shadowJar { exclude("it/unimi/dsi/fastutil/**") }
