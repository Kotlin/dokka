/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
    id("org.jetbrains.conventions.maven-cli-setup")
}

dependencies {
    implementation(projects.integrationTestUtilities)

    implementation(kotlin("test-junit5"))
}

val dokkaSubprojects = gradle.includedBuild("dokka-subprojects")
val mavenPlugin = gradle.includedBuild("maven-plugin")

tasks.integrationTest {
    dependsOn(
        dokkaSubprojects.task(":publishToMavenLocal"),
        mavenPlugin.task(":publishToMavenLocal"),
    )
    dependsOn(tasks.installMavenBinary)
    val mvn = mavenCliSetup.mvn
    inputs.file(mvn)

    environment("DOKKA_VERSION", project.version)
    doFirst("workaround for https://github.com/gradle/gradle/issues/24267") {
        environment("MVN_BINARY_PATH", mvn.get().asFile.invariantSeparatorsPath)
    }
}
