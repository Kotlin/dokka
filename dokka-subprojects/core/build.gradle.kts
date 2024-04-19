/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("dokka-core")

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.xml)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
}

tasks.processResources {
    val dokkaVersion = dokkaBuild.projectVersion
    inputs.property("dokkaVersion", dokkaVersion)

    eachFile {
        if (name == "dokka-version.properties") {
            expand(
                "dokkaVersion" to dokkaVersion.get()
            )
        }
    }
}
