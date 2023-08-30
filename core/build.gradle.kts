/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokkaVersion
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

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
    testImplementation(projects.core.testApi)
}

tasks {
    processResources {
        inputs.property("dokkaVersion", dokkaVersion)
        eachFile {
            if (name == "dokka-version.properties") {
                filter { line ->
                    line.replace("<dokka-version>", dokkaVersion)
                }
            }
        }
    }
}

registerDokkaArtifactPublication("dokkaCore") {
    artifactId = "dokka-core"
}
