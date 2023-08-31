/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("org.jetbrains.conventions.dokka-html-frontend-files")
    id("org.jetbrains.conventions.base-unit-test")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.subprojects.analysisMarkdownJb)

    // Other
    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.freemarker)
    implementation(libs.kotlinx.html)
    implementation(libs.jackson.kotlin)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    // Test only
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiterParams)

    symbolsTestConfiguration(project(path = ":subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestConfiguration(project(path = ":subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.plugins.base.baseTestUtils) {
        exclude(module = "analysis-kotlin-descriptors")
    }
    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(projects.core.testApi)

    dokkaHtmlFrontendFiles(projects.plugins.base.frontend) {
        because("fetch frontend files from subproject :plugins:base:frontend")
    }
}

// access the frontend files via the dependency on :plugins:base:frontend
val dokkaHtmlFrontendFiles: Provider<FileCollection> =
    configurations.dokkaHtmlFrontendFiles.map { frontendFiles ->
        frontendFiles.incoming.artifacts.artifactFiles
    }

val preparedokkaHtmlFrontendFiles by tasks.registering(Sync::class) {
    description = "copy Dokka Base frontend files into the resources directory"

    from(dokkaHtmlFrontendFiles) {
        include("*.js")
        into("dokka/scripts")
    }

    from(dokkaHtmlFrontendFiles) {
        include("*.css")
        into("dokka/styles")
    }

    into(layout.buildDirectory.dir("generated/src/main/resources"))
}

sourceSets.main {
    resources.srcDir(preparedokkaHtmlFrontendFiles.map { it.destinationDir })
}

tasks.test {
    maxHeapSize = "4G"
}

registerDokkaArtifactPublication("dokkaBase") {
    artifactId = "dokka-base"
}
