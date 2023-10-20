/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.overridePublicationArtifactId

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.publishing-default")
}

// TODO [structure-refactoring]  may be it's not needed, as we have same project name
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
    testImplementation(projects.coreTestApi)
}

tasks {
    processResources {
        inputs.property("dokkaVersion", project.version)
        eachFile {
            if (name == "dokka-version.properties") {
                filter { line ->
                    line.replace("<dokka-version>", project.version.toString())
                }
            }
        }
    }
}
