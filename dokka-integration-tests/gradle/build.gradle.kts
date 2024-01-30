import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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

val aggregatingProject = gradle.includedBuild("dokka")

tasks.integrationTest {
    dependsOn(aggregatingProject.task(":publishToMavenLocal"))

    environment("DOKKA_VERSION", project.version)

    inputs.dir(file("projects"))
        .withPropertyName("projectsDir")
        .withPathSensitivity(RELATIVE)

    javaLauncher.set(javaToolchains.launcherFor {
        // kotlinx.coroutines requires Java 11+
        languageVersion.set(dokkaBuild.testJavaLauncherVersion.map {
            maxOf(it, JavaLanguageVersion.of(11))
        })
    })
}
