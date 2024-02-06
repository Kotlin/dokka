/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.tasks.GitCheckoutTask
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("dokkabuild.test-integration")
}

dependencies {
    implementation(projects.utilities)

    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
    implementation(libs.junit.jupiterParams)

    implementation(gradleTestKit())

    implementation(libs.jsoup)
}

kotlin {
    // this project only contains test utils and isn't published, so it doesn't matter about explicit API
    explicitApi = ExplicitApiMode.Disabled
}

val templateProjectsDir = layout.projectDirectory.dir("projects")

val aggregatingProject = gradle.includedBuild("dokka")

tasks.integrationTest {
    dependsOn(aggregatingProject.task(":publishToMavenLocal"))
    dependsOn(
        checkoutKotlinxCoroutines,
        checkoutKotlinxSerialization,
    )

    environment("DOKKA_VERSION", project.version)

    inputs.dir(templateProjectsDir)

    javaLauncher.set(javaToolchains.launcherFor {
        // kotlinx.coroutines requires Java 11+
        languageVersion.set(dokkaBuild.testJavaLauncherVersion.map {
            maxOf(it, JavaLanguageVersion.of(11))
        })
    })
}

val checkoutKotlinxCoroutines by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/Kotlin/kotlinx.coroutines.git"
    commitId = "b78bbf518bd8e90e9ed2133ebdacc36441210cd6"
    destination = templateProjectsDir.dir("coroutines/kotlinx-coroutines")
}

val checkoutKotlinxSerialization by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/Kotlin/kotlinx.serialization.git"
    commitId = "ed1b05707ec27f8864c8b42235b299bdb5e0015c"
    destination = templateProjectsDir.dir("serialization/kotlinx-serialization")
}
