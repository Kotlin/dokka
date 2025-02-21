/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId
import dokkabuild.utils.downloadKotlinStdlibJvmSources
import dokkabuild.utils.systemProperty
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.setup-html-frontend-files")
    id("dokkabuild.test-k2")
}

overridePublicationArtifactId("dokka-base")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.analysisKotlinApi)

    // kdp start
    implementation(projects.kotlinDocumentation.kotlinDocumentationModel)
    // kdp end

    implementation(projects.dokkaSubprojects.analysisMarkdownJb)

    // Other
    implementation(libs.kotlin.reflect)
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
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiterParams)

    symbolsTestImplementation(project(path = ":dokka-subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestImplementation(project(path = ":dokka-subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.dokkaSubprojects.pluginBaseTestUtils)
    testImplementation(projects.dokkaSubprojects.coreContentMatcherTestUtils)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
    testImplementation(projects.dokkaSubprojects.analysisKotlinApi)

    dokkaHtmlFrontendFiles(projects.dokkaSubprojects.pluginBaseFrontend) {
        because("fetch frontend files from subproject :plugin-base-frontend")
    }
}

// access the frontend files via the dependency on :plugins:base:frontend
val dokkaHtmlFrontendFiles: Provider<FileCollection> =
    configurations.dokkaHtmlFrontendFiles.map { frontendFiles ->
        frontendFiles.incoming.artifacts.artifactFiles
    }

val prepareDokkaHtmlFrontendFiles by tasks.registering(Sync::class) {
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

    outputs.cacheIf("always cache: avoid fetching files from another subproject") { true }
}

sourceSets.main {
    resources.srcDir(prepareDokkaHtmlFrontendFiles.map { it.destinationDir })
}

tasks.test {
    maxHeapSize = "4G"
}

//region Download and unpack kotlin-stdlib, so EnumTemplatesTest can test synthetic enum functions.
val kotlinStdlibSourcesDir = downloadKotlinStdlibJvmSources(project)
tasks.withType<Test>().configureEach {
    systemProperty
        .inputDirectory("kotlinStdlibSourcesDir", kotlinStdlibSourcesDir)
        .withPathSensitivity(RELATIVE)
}
//endregion
