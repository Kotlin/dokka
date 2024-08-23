/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
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

    listOf(
        libs.kotlin.analysis.api.api,
        libs.kotlin.analysis.api.standalone,
    ).forEach {
        implementation(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    listOf(
        libs.kotlin.analysis.api.impl,
        libs.kotlin.analysis.api.fir,
        libs.kotlin.low.level.api.fir,
        libs.kotlin.analysis.api.platform,
        libs.kotlin.symbol.light.classes,
    ).forEach {
        runtimeOnly(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    // copy-pasted from Analysis API https://github.com/JetBrains/kotlin/blob/a10042f9099e20a656dec3ecf1665eea340a3633/analysis/low-level-api-fir/build.gradle.kts#L37
    runtimeOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")

    runtimeOnly(libs.kotlinx.collections.immutable)
    implementation(libs.kotlin.compiler.k2) {
        isTransitive = false
    }

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}

tasks.shadowJar {
    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}
