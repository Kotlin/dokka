/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dependsOnMavenLocalPublication

plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
}

dependencies {
    implementation(projects.integrationTests)

    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
    implementation(libs.junit.jupiterParams)

    implementation(gradleTestKit())

    implementation(libs.jsoup)
}

tasks.integrationTest {
    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    inputs.dir(file("projects"))
    dependsOnMavenLocalPublication()

    javaLauncher.set(javaToolchains.launcherFor {
        // kotlinx.coroutines requires Java 11+
        languageVersion.set(dokkaBuild.testJavaLauncherVersion.map {
            maxOf(it, JavaLanguageVersion.of(11))
        })
    })
}
