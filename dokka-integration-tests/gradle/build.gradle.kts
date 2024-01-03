/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.test-integration")
    id("dokkabuild.testing.android-setup")
}

dependencies {
    implementation(projects.utilities)

    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
    implementation(libs.junit.jupiterParams)

    implementation(gradleTestKit())

    implementation(libs.jsoup)
}

val aggregatingProject = gradle.includedBuild("dokka")

tasks.integrationTest {
    // pass the property to a test fork
    project.findProperty("org.jetbrains.dokka.experimental.tryK2")
        ?.let { systemProperty("org.jetbrains.dokka.experimental.tryK2", it) }

    dependsOn(aggregatingProject.task(":publishToMavenLocal"))

    environment("DOKKA_VERSION", project.version)

    inputs.dir(file("projects"))

    javaLauncher.set(javaToolchains.launcherFor {
        // kotlinx.coroutines requires Java 11+
        languageVersion.set(dokkaBuild.testJavaLauncherVersion.map {
            maxOf(it, JavaLanguageVersion.of(11))
        })
    })
}

val templateProjectsDir = layout.projectDirectory.dir("projects")
val androidSdkDir = providers
    // first try getting pre-installed SDK (e.g. via GitHub step setup-android)
    .environmentVariable("ANDROID_SDK_ROOT").map(::File)
    .orElse(providers.environmentVariable("ANDROID_HOME").map(::File))
    // else get the project-local SDK
    .orElse(templateProjectsDir.dir("ANDROID_SDK").asFile)

tasks.withType<Test>().configureEach {
    environment("ANDROID_HOME", androidSdkDir.get().invariantSeparatorsPath)
}

tasks.createAndroidLocalPropertiesFiles {
    androidProjectsDirectories.from(
        // directories of Android projects that require a local.properties file
        templateProjectsDir.dir("it-android-0"),
    )
    androidSdkDirPath.set(androidSdkDir.map { it.invariantSeparatorsPath })
}

tasks.integrationTestPreparation {
    dependsOn(tasks.createAndroidLocalPropertiesFiles)
}
