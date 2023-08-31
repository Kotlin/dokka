/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)

    implementation(libs.jsoup)
    implementation(libs.jetbrains.markdown)
}

registerDokkaArtifactPublication("analysisMarkdown") {
    artifactId = "analysis-markdown"
}
