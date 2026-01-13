/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import java.nio.file.Path
import kotlin.io.path.readText

class SuppressKGPGeneratedFilesFunctionalTest : FunSpec({

    context("suppressGeneratedFiles") {

        test("when suppressGeneratedFiles is true, generated files should be suppressed") {
            val project = createProject(suppressGeneratedFiles = true)

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--stacktrace",
                )
                .build {
                    val htmlContent = project.projectDir
                        .resolve("build/dokka/html/suppress-kgp-generated-files-true/[root]/index.html")
                        .also(Path::shouldExist)
                        .readText()
                    htmlContent shouldContain "MyClass"
                    htmlContent shouldNotContain "GeneratedClass"
                }
        }

        test("when suppressGeneratedFiles is false, generated files should be documented") {
            val project = createProject(suppressGeneratedFiles = false)

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun-tasks",
                    "--stacktrace",
                )
                .build {
                    val htmlContent = project.projectDir
                        .resolve("build/dokka/html/suppress-kgp-generated-files-false/[root]/index.html")
                        .also(Path::shouldExist)
                        .readText()
                    htmlContent shouldContain "MyClass"
                    htmlContent shouldContain "GeneratedClass"
                }
        }
    }
})

private fun createProject(
    suppressGeneratedFiles: Boolean
): GradleProjectTest = gradleKtsProjectTest("suppress-kgp-generated-files-$suppressGeneratedFiles") {
    buildGradleKts = """
        |plugins {
        |    kotlin("jvm") version "2.3.0" // KGP 2.3.0 is the first version that contains `generatedKotlin` API
        |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
        |}
        |
        |dokka {
        |    dokkaSourceSets.configureEach {
        |        suppressGeneratedFiles.set(${suppressGeneratedFiles})
        |    }
        |}
        |// add generated sources via KGP API
        |kotlin.sourceSets.named("main") {
        |    generatedKotlin.srcDir("src/gen/kotlin")
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

    dir("src/gen/kotlin") {
        createKotlinFile(
            "GeneratedClass.kt",
            """
            |/**
            | * A generated class that should NOT be documented
            | */
            |class GeneratedClass {
            |    /**
            |     * A generated function that should NOT be documented
            |     */
            |    fun generatedFunction() = "Generated"
            |}
            |""".trimMargin()
        )
    }
}
