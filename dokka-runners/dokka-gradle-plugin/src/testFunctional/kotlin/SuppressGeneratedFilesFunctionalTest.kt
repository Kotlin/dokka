/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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

class SuppressGeneratedFilesFunctionalTest : FunSpec({

    context("suppressGeneratedFiles") {

        test("when suppressGeneratedFiles is true, generated files should be suppressed") {
            val project = createProject(suppressGeneratedFiles = true)

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun-tasks",
                    "--stacktrace",
                )
                .build {
                    val htmlContent = project.projectDir
                        .resolve("build/dokka/html/suppress-generated-files-true/[root]/index.html")
                        .also(Path::shouldExist)
                        .readText()
                    htmlContent shouldContain "MyClass"
                    htmlContent shouldNotContain "GeneratedClass"
                    htmlContent shouldNotContain "GeneratedJavaClass"
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
                        .resolve("build/dokka/html/suppress-generated-files-false/[root]/index.html")
                        .also(Path::shouldExist)
                        .readText()
                    htmlContent shouldContain "MyClass"
                    htmlContent shouldContain "GeneratedClass"
                    htmlContent shouldContain "GeneratedJavaClass"
                }
        }
    }
})

private fun createProject(
    suppressGeneratedFiles: Boolean
): GradleProjectTest = gradleKtsProjectTest("suppress-generated-files-$suppressGeneratedFiles") {
    buildGradleKts = """
        |plugins {
        |    kotlin("jvm") version embeddedKotlinVersion
        |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
        |}
        |
        |dokka {
        |    dokkaSourceSets.configureEach {
        |        suppressGeneratedFiles.set(${suppressGeneratedFiles})
        |        // emulate generated sources added, for example by KSP
        |        sourceRoots.from("build/generated")
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

    dir("build/generated/ksp/main") {
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

    dir("build/generated/apt/java/main") {
        createFile(
            "GeneratedJavaClass.java",
            """
            |/**
            | * A generated Java class that should NOT be documented
            | */
            |public class GeneratedJavaClass {
            |    /**
            |     * A generated method that should NOT be documented
            |     */
            |    public String generatedMethod() {
            |        return "Generated Java";
            |    }
            |}
            |""".trimMargin()
        )
    }
}
