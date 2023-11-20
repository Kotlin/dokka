/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("analysis-markdown")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    implementation(libs.jsoup)
    implementation(libs.jetbrains.markdown)
}
