/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    api(projects.core)

    implementation(kotlin("reflect"))
}

registerDokkaArtifactPublication("dokkaTestApi") {
    artifactId = "dokka-test-api"
}
