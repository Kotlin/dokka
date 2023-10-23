/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
}

dependencies {
    implementation(projects.integrationTestUtilities)

    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
    implementation(libs.junit.jupiterParams)

    implementation(gradleTestKit())

    implementation(libs.jsoup)
}

val dokkaSubprojects = gradle.includedBuild("dokka-subprojects")
val gradlePluginClassic = gradle.includedBuild("gradle-plugin-classic")

tasks.integrationTest {
    dependsOn(
        dokkaSubprojects.task(":publishToMavenLocal"),
        gradlePluginClassic.task(":publishToMavenLocal"),
    )
    environment("DOKKA_VERSION", project.version)
    inputs.dir(file("projects"))

    javaLauncher.set(javaToolchains.launcherFor {
        // kotlinx.coroutines requires Java 11+
        languageVersion.set(dokkaBuild.testJavaLauncherVersion.map {
            maxOf(it, JavaLanguageVersion.of(11))
        })
    })
}
