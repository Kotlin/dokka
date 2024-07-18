/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("dokka-test-api")

dependencies {
    api(projects.dokkaSubprojects.dokkaCore)
    // for assertions over `DokkaConfiguration/DokkaSourceSet`
    // it's compileOnly, so it should be able to handle both `junit` and `junit5`
    compileOnly(kotlin("test"))

    implementation(kotlin("reflect"))
}
