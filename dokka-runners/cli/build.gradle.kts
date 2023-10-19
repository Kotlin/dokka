/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")

    // TODO [structure-refactoring] this plugin should not contain the version, it's declared in build-logic
    // for some reason, it doesn't want to be resolved without the version, even though it works in other subprojects
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-core")
    implementation(libs.kotlinx.cli)

    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-cli-$dokka_version.jar")
        archiveClassifier.set("")
        manifest {
            attributes("Main-Class" to "org.jetbrains.dokka.MainKt")
        }
    }
}

registerDokkaArtifactPublication("dokkaCli") {
    artifactId = "dokka-cli"
    component = Shadow
}
