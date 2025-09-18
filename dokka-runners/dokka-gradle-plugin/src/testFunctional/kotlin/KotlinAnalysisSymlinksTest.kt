/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.sequences.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContainIgnoringCase
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText

class KotlinAnalysisSymlinksTest : FunSpec({

    context("when DGP generates HTML") {
        val project = initProject()

        project.runner
            .addArguments(
                ":dokkaGeneratePublicationHtml",
                "--rerun-tasks",
            )
            .forwardOutput()
            .build {
                test("expect build is successful") {
                    output shouldContain "BUILD SUCCESSFUL"
                }
            }

        test("expect all DGP workers are successful") {
            project
                .findFiles { it.name == "dokka-worker.log" }
                .shouldBeSingleton { dokkaWorkerLog ->
                    dokkaWorkerLog.shouldBeAFile()
                    dokkaWorkerLog.readText().shouldNotContainAnyOf(
                        "[ERROR]",
                        "[WARN]",
                    )
                }
        }

        context("expect HTML site is generated") {
            val projectName = "kotlin-analysis-symlinks-project"

            test("with expected HTML files") {
                project.projectDir
                    .resolve("build/dokka/")
                    .listRelativePathsMatching { it.extension == "html" }
                    .shouldContainExactlyInAnyOrder(
                        "html/index.html",
                        "html/navigation.html",
                        "html/$projectName/[root]/-foo/-foo.html",
                        "html/$projectName/[root]/-foo/foo.html",
                        "html/$projectName/[root]/-foo/index.html",
                        "html/$projectName/[root]/index.html",
                        "html/$projectName/[root]/use-foo.html",
                    )
            }

            test("expect no 'unknown class' message in HTML files") {
                val htmlFiles = project.projectDir.toFile()
                    .resolve("build/dokka/html")
                    .walk()
                    .filter { it.isFile && it.extension == "html" }

                htmlFiles.shouldNotBeEmpty()

                htmlFiles.forEach { htmlFile ->
                    val relativePath = htmlFile.relativeTo(project.projectDir.toFile())
                    withClue("$relativePath should not contain 'Error class: unknown class' or 'ERROR CLASS'") {
                        htmlFile.useLines { lines ->
                            lines.shouldForAll { line -> line.shouldNotContainIgnoringCase("ERROR CLASS") }
                        }
                    }
                }
            }
        }
    }
})


private fun initProject(): GradleProjectTest = gradleKtsProjectTest("kotlin-analysis-symlinks-project") {
    buildGradleKts = """
        |plugins {
        |  kotlin("jvm") version embeddedKotlinVersion
        |  id("org.jetbrains.dokka") version "$DOKKA_VERSION"
        |}
        """.trimMargin()

    createKotlinFile(
        filePath = "src/symlinked/kotlin/foo/Foo.kt",
        contents = """
            |package foo
            |
            |class Foo {
            |  fun foo() = Unit
            |}
            """.trimIndent()
    )

    // should be able to access `Foo`
    createKotlinFile(
        filePath = "src/main/kotlin/project/UseFoo.kt",
        contents = """
            |package project
            |
            |import foo.Foo
            |
            |/** Uses [Foo] */
            |fun useFoo(foo: Foo) = Unit
            """.trimIndent()
    )

    // could exist because of caching in tests...
    Files.deleteIfExists(file("src/main/kotlin/foo"))
    Files.createSymbolicLink(
        file("src/main/kotlin/foo"),
        file("src/symlinked/kotlin/foo")
    )
}
