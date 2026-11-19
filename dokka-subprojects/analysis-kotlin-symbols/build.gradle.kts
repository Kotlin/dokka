/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-shadow")
}

overridePublicationArtifactId("analysis-kotlin-symbols")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    // this is a `hack` to include classes `intellij-java-psi-api` in shadowJar
    // which are not present in `kotlin-compiler`
    // should be `api` since we already have it in :analysis-java-psi
    // it's harder to do it in the same as with `fastutil`
    // as several intellij dependencies share the same packages like `org.intellij.core`
    api(libs.intellij.java.psi.api) { isTransitive = false }

    implementation(projects.dokkaSubprojects.analysisKotlinApi)
    implementation(projects.dokkaSubprojects.analysisMarkdownJb)
    implementation(projects.dokkaSubprojects.analysisJavaPsi) {
        // a stripped one is already presented in `kotlin-compiler`
        // also, it prevents using `aalto-xml` in `jackson` by excluding Stax API services files
       exclude("com.fasterxml", "aalto-xml")
        // `kotlinx-collections-immutable` is embedded (shaded under its original package) in `kotlin-compiler`.
        // The IntelliJ platform pulls in a separate, older external artifact transitively, which shadows the
        // embedded one on the classpath and lacks `PersistentList.adding` (renamed from `add` in 0.5.0),
        // causing a NoSuchMethodError in KaBaseLifetimeTracker. Exclude it so the embedded copy is used.
       exclude("org.jetbrains.kotlinx", "kotlinx-collections-immutable-jvm")
    }

    // ----------- Analysis dependencies ----------------------------------------------------------------------------

    implementation(libs.kotlin.analysis.api)
    // should be runtimeOnly, but it's needed for kotlinLightMethodChecker (`org.jetbrains.kotlin.asJava.*` classes)
    // and the other platform classes (org.jetbrains.kotlin.platform.*)
    // also, see KT-91823
    compileOnly(libs.kotlin.analysis.api.impl)

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)

    // to gain access to `com.intellij.openapi.util.NlsSafe` (and other annotation classes referenced
    // by the inferred types of the Analysis API) at compile time in KtExpression.text
    compileOnly(libs.intellij.util.base)
}

tasks.shadowJar {
    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}
