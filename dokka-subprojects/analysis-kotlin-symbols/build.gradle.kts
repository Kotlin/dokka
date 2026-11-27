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
    implementation(projects.dokkaSubprojects.analysisJavaPsi)

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
    mergeServiceFiles {
        // `aalto-xml` (shaded alongside the K2 compiler classes) registers itself as the
        // `javax.xml.stream.*` factory provider via these service files. On the fat Dokka
        // classpath that would hijack StAX resolution for the whole host application —
        // keep the aalto classes (the compiler references them directly) but drop the
        // registrations, so `XMLInputFactory.newInstance()` falls back to the JDK.
        // NB: the exclusion must be on the transformer (`mergeServiceFiles`) too, not only
        // on the task — the transformer re-injects service files after content filtering.
        exclude("javax.xml.stream.*")
    }
    exclude("META-INF/services/javax.xml.stream.*")
}
