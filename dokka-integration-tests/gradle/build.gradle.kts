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
val androidSdkDir = templateProjectsDir.dir("ANDROID_SDK")

tasks.withType<Test>().configureEach {
    environment("ANDROID_HOME", androidSdkDir.asFile.invariantSeparatorsPath)
}

val updateProjectsAndroidLocalProperties by tasks.registering {
    description = "updates the local.properties file in each test project, so the local ANDROID_SDK dir is used"

    // The names of android projects that require a local.properties file
    val androidProjects = setOf(
        "it-android-0",
    )

    // find all Android projects that need a local.properties file
    val androidProjectsDirectories = templateProjectsDir.asFile.walk()
        .filter { it.isDirectory && it.name in androidProjects }

    // determine the task outputs for up-to-date checks
    val propertyFileDestinations = androidProjectsDirectories.map { project ->
        project.resolve("local.properties")
    }
    outputs.files(propertyFileDestinations.toList()).withPropertyName("propertyFileDestinations")

    // the source local.properties file
    val sourcePropertyFile = tasks.createAndroidLocalPropertiesFile.flatMap { it.localPropertiesFile.asFile }
    inputs.file(sourcePropertyFile).withPropertyName("sourcePropertyFile").normalizeLineEndings()

    doLast("update local.properties files") {
        val src = sourcePropertyFile.get().readText()

        propertyFileDestinations.forEach { dst ->
            dst.createNewFile()
            dst.writeText(src)
        }
    }
}

tasks.integrationTestPreparation {
    dependsOn(updateProjectsAndroidLocalProperties)
}
