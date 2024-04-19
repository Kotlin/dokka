/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("versioning-plugin")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    implementation(projects.dokkaSubprojects.pluginBase)
    implementation(projects.dokkaSubprojects.pluginTemplating)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.html)
    implementation(libs.apacheMaven.artifact)
    implementation(libs.jackson.kotlin)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
}
