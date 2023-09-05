import org.jetbrains.registerDokkaArtifactPublication

/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    api("org.jetbrains.dokka:dokka-core:1.9.0")
}

registerDokkaArtifactPublication("dokkaCore") {
    artifactId = "dokka-core"
}
