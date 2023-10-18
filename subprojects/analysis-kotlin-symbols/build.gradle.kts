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
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.subprojects.analysisMarkdownJb)
    implementation(projects.subprojects.analysisJavaPsi)


    // ----------- IDE dependencies ----------------------------------------------------------------------------

    listOf(
        libs.intellij.platform.util.rt,
        libs.intellij.platform.util.api,
        libs.intellij.java.psi.api,
        libs.intellij.java.psi.impl
    ).forEach {
        runtimeOnly(it) { isTransitive = false }
    }

    implementation(libs.intellij.java.psi.api) { isTransitive = false }


    // TODO move to toml
    listOf(
        "com.jetbrains.intellij.platform:util-class-loader",
        "com.jetbrains.intellij.platform:util-text-matching",
        "com.jetbrains.intellij.platform:util-base",
        "com.jetbrains.intellij.platform:util-xml-dom",
        "com.jetbrains.intellij.platform:core-impl",
        "com.jetbrains.intellij.platform:extensions",
    ).forEach {
        runtimeOnly("$it:213.7172.25") { isTransitive = false }
    }

    implementation("com.jetbrains.intellij.platform:core:213.7172.25") {
        isTransitive = false
    } // for Standalone prototype

    // ----------- Analysis dependencies ----------------------------------------------------------------------------

    listOf(
        libs.kotlin.high.level.api.api,
        libs.kotlin.analysis.api.standalone,
    ).forEach {
        implementation(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    listOf(
        libs.kotlin.high.level.api.impl,
        libs.kotlin.high.level.api.fir,
        libs.kotlin.high.level.api.fe10,
        libs.kotlin.low.level.api.fir,
        libs.kotlin.analysis.project.structure,
        libs.kotlin.analysis.api.providers,
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

tasks {
    shadowJar {
        val dokka_version: String by project

        // cannot be named exactly like the artifact (i.e analysis-kotlin-symbols-VER.jar),
        // otherwise leads to obscure test failures when run via CLI, but not via IJ
        archiveFileName.set("analysis-kotlin-symbols-all-$dokka_version.jar")
        archiveClassifier.set("")

        // service files are merged to make sure all Dokka plugins
        // from the dependencies are loaded, and not just a single one.
        mergeServiceFiles()
    }
}

registerDokkaArtifactPublication("analysisKotlinSymbols") {
    artifactId = "analysis-kotlin-symbols"
    component = DokkaPublicationBuilder.Component.Shadow
}
