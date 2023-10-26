/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
    implementation(projects.analysisKotlinApi)
    implementation(projects.analysisKotlinDescriptorsCompiler)
    implementation(projects.analysisKotlinDescriptorsIde)
}

tasks.shadowJar {
    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}
