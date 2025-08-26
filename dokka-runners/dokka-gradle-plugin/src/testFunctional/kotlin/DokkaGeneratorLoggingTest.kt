/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*

class DokkaGeneratorLoggingTest : FunSpec({

    context("DokkaGenerator logging:") {
        val project = createProject()

        test("at lifecycle log level expect only error and warn logs") {

            project.runner
                .addArguments(
                    ":dokkaGenerateModuleHtml",
                    "--rerun",
                )
                .build {
                    output.invariantNewlines() shouldContain """
                        > Task :dokkaGenerateModuleHtml
                    """.trimIndent()

                    output.invariantNewlines() shouldContain """
                        e: [:dokkaGenerateModuleHtml] test error message
                        w: [:dokkaGenerateModuleHtml] test warn message
                        """.trimIndent()

                    output.shouldNotContainAnyOf(
                        "test info message",
                        "test debug message",
                        "test progress message",
                    )
                }

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun",
                )
                .build {
                    output.invariantNewlines() shouldContain """
                        > Task :dokkaGeneratePublicationHtml
                        """.trimIndent()

                    output.invariantNewlines() shouldContain """
                        e: [:dokkaGeneratePublicationHtml] test error message
                        w: [:dokkaGeneratePublicationHtml] test warn message
                        """.trimIndent()

                    output.shouldNotContainAnyOf(
                        "test info message",
                        "test debug message",
                        "test progress message",
                    )
                }
        }

        test("at info log level expect all non-debug DokkaGenerator logs") {

            project.runner
                .addArguments(
                    ":dokkaGenerateModuleHtml",
                    "--rerun",
                    "--info",
                )
                .build {
                    output.invariantNewlines() shouldContain """
                        e: [:dokkaGenerateModuleHtml] test error message
                        w: [:dokkaGenerateModuleHtml] test warn message
                        [:dokkaGenerateModuleHtml] test info message
                        [:dokkaGenerateModuleHtml] test progress message
                        """.trimIndent()

                    output shouldNotContain "test debug message"
                }

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun",
                    "--info",
                )
                .build {
                    output.invariantNewlines() shouldContain """
                        e: [:dokkaGeneratePublicationHtml] test error message
                        w: [:dokkaGeneratePublicationHtml] test warn message
                        [:dokkaGeneratePublicationHtml] test info message
                        [:dokkaGeneratePublicationHtml] test progress message
                        """.trimIndent()

                    output shouldNotContain "test debug message"
                }
        }

        test("at debug log level expect all DokkaGenerator logs") {

            project.runner
                .addArguments(
                    ":dokkaGenerateModuleHtml",
                    "--rerun",
                    "--debug",
                )
                .build {
                    output
                        .lineSequence()
                        .filter { "[:dokkaGenerateModuleHtml]" in it }
                        .map { it.substringAfter(" ") } // drop the timestamp from the log message
                        .toList()
                        .shouldContainAll(
                            "[ERROR] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] e: [:dokkaGenerateModuleHtml] test error message",
                            "[WARN] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] w: [:dokkaGenerateModuleHtml] test warn message",
                            "[DEBUG] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] [:dokkaGenerateModuleHtml] test debug message",
                            "[INFO] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] [:dokkaGenerateModuleHtml] test info message",
                            "[INFO] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] [:dokkaGenerateModuleHtml] test progress message",
                        )
                }

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun",
                    "--debug",
                )
                .build {
                    output
                        .lineSequence()
                        .filter { "[:dokkaGeneratePublicationHtml]" in it }
                        .map { it.substringAfter(" ") } // drop the timestamp from the log message
                        .toList()
                        .shouldContainAll(
                            "[ERROR] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] e: [:dokkaGeneratePublicationHtml] test error message",
                            "[WARN] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] w: [:dokkaGeneratePublicationHtml] test warn message",
                            "[DEBUG] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] [:dokkaGeneratePublicationHtml] test debug message",
                            "[INFO] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] [:dokkaGeneratePublicationHtml] test info message",
                            "[INFO] [org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker] [:dokkaGeneratePublicationHtml] test progress message",
                        )
                }
        }
    }
})

/**
 * Create a test project with a custom [org.jetbrains.dokka.plugability.DokkaPlugin] that logs some test messages.
 */
private fun createProject(): GradleProjectTest = gradleKtsProjectTest("dokka-generator-logging") {

    buildGradleKts = """
        |import org.jetbrains.dokka.gradle.tasks.*
        |
        |plugins {
        |    kotlin("jvm") version embeddedKotlinVersion
        |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
        |}
        |
        |dependencies {
        |    dokkaPlugin(project(":dokka-logger-test-plugin"))
        |}
        |
        """.trimMargin()

    settingsGradleKts += """
        |include(":dokka-logger-test-plugin")        
        |
        """.trimMargin()

    createKotlinFile("src/main/kotlin/Foo.kt", "class Foo")

    dir("dokka-logger-test-plugin") {
        buildGradleKts = """
            |plugins {
            |    kotlin("jvm")
            |}
            |
            |dependencies {
            |    compileOnly("org.jetbrains.dokka:dokka-core:$DOKKA_VERSION")
            |    compileOnly("org.jetbrains.dokka:dokka-base:$DOKKA_VERSION")
            |}
            """.trimMargin()

        createKotlinFile(
            "src/main/kotlin/DokkaLoggerTestPlugin.kt", """
            |package logtest
            |
            |import org.jetbrains.dokka.*
            |import org.jetbrains.dokka.plugability.*
            |import org.jetbrains.dokka.validity.*
            |
            |class DokkaLoggerTestPlugin : DokkaPlugin() {
            |
            |    @DokkaPluginApiPreview
            |    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
            |
            |    internal val logSomeMessages by extending {
            |        CoreExtensions.preGenerationCheck providing ::LogSomeMessages
            |    }
            |}
            |
            |class LogSomeMessages(private val context: DokkaContext) : PreGenerationChecker {
            |
            |    override fun invoke(): PreGenerationCheckerOutput {
            |
            |        context.logger.error("test error message")
            |        context.logger.warn("test warn message")
            |        context.logger.debug("test debug message")
            |        context.logger.info("test info message")
            |        context.logger.progress("test progress message")
            |
            |        return PreGenerationCheckerOutput(true, emptyList())
            |    }
            |}
            |
            """.trimMargin()
        )

        createFile(
            "src/main/resources/META-INF/services/org.jetbrains.dokka.plugability.DokkaPlugin",
            "logtest.DokkaLoggerTestPlugin",
        )
    }
}
