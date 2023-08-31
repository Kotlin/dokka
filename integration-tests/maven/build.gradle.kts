/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dependsOnMavenLocalPublication

plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
    id("org.jetbrains.conventions.maven-cli-setup")
}

dependencies {
    implementation(projects.integrationTests)

    implementation(kotlin("test-junit5"))
}

tasks.integrationTest {
    dependsOnMavenLocalPublication()

    dependsOn(tasks.installMavenBinary)
    val mvn = mavenCliSetup.mvn
    inputs.file(mvn)

    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    doFirst("workaround for https://github.com/gradle/gradle/issues/24267") {
        environment("MVN_BINARY_PATH", mvn.get().asFile.invariantSeparatorsPath)
    }
}
