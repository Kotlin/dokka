/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*

class AndroidIntersectedSourceRootsErrorTest : FreeSpec({
    // Dokka project Gradle wrapper uses Gradle 8.14, which is used by default when running functional tests.
    // But AGP 9+ requires Gradle 9.1+, so we need to override it for tests
    // Once the Dokka project Gradle wrapper version is updated to 9.1+, we can remove this override
    val gradleVersion = "9.1.0"
    // In the Dokka project we run tests on CI (GitHub Actions) with different JDK versions, 8 by default.
    // But Gradle 9+ requires JDK 17 to run
    // Once we run tests on CI with JDK 17+, we can remove this check
    val javaVersion = when (val specVersion = System.getProperty("java.specification.version")) {
        "1.8" -> 8
        else -> specVersion.toInt()
    }
    val usesJava17 = javaVersion >= 17

    "when project has multiple flavors".config(enabled = usesJava17) {
        val project = initAndroidProject(selectedVariantName = null)

        project.runner
            .withGradleVersion(gradleVersion)
            .addArguments(
                ":dokkaGeneratePublicationHtml",
                "--rerun-tasks",
                "--stacktrace",
            )
            .forwardOutput()
            .buildAndFail {
                output shouldContain "Dokka cannot generate documentation for Android projects with multiple enabled variants that have common source roots."
                output shouldNotContain "Pre-generation validity check failed: Source sets 'freeRelease' and 'paidRelease' have the common source roots"
            }
    }

    "when project has multiple flavors and single selected variant".config(enabled = usesJava17) {
        val project = initAndroidProject(selectedVariantName = "freeRelease")

        project.runner
            .withGradleVersion(gradleVersion)
            .addArguments(
                ":dokkaGeneratePublicationHtml",
                "--rerun-tasks",
                "--stacktrace",
            )
            .forwardOutput()
            .build {
                output shouldNotContain "Dokka cannot generate documentation for Android projects with multiple enabled variants that have common source roots."
                output shouldNotContain "Pre-generation validity check failed: Source sets 'freeRelease' and 'paidRelease' have the common source roots"
            }
    }
})

private fun initAndroidProject(
    selectedVariantName: String?
): GradleProjectTest = gradleKtsProjectTest("android-project-$selectedVariantName") {
    buildGradleKts = """
        |plugins {
        |    id("com.android.library") version "9.0.1"
        |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
        |}
        |android {
        |    compileSdk = 34
        |    namespace = "dokka.test.android.project"
        |    flavorDimensions.add("price")
        |    productFlavors.create("paid").dimension = "price"
        |    productFlavors.create("free").dimension = "price"
        |}
        |""".trimMargin()

    if (selectedVariantName != null) buildGradleKts += """
        |dokka {
        |    dokkaSourceSets.configureEach {
        |        suppress.set(name != "$selectedVariantName")
        |    }
        |}
        |""".trimMargin()

    dir("src/main/kotlin") {
        createKotlinFile(
            "MyClass.kt",
            """
            |/**
            | * A regular class that should be documented
            | */
            |class MyClass {
            |    /**
            |     * A function that should be documented
            |     */
            |    fun regularFunction() = "Hello"
            |}
            |""".trimMargin()
        )
    }
}
