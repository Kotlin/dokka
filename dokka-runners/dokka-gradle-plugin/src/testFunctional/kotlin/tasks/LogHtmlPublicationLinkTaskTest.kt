/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.gradle.internal.DokkatooConstants
import org.jetbrains.dokka.gradle.tasks.LogHtmlPublicationLinkTask.Companion.ENABLE_TASK_PROPERTY_NAME
import org.jetbrains.dokka.gradle.utils.*

class LogHtmlPublicationLinkTaskTest : FunSpec({

    context("given an active file-host server") {
        val server = embeddedServer(CIO, port = 0) { }
        server.start(wait = false)
        val serverPort = server.resolvedConnectors().first().port

        val validServerUri = "http://localhost:$serverPort"
        val validServerUriParam = `-P`("testServerUri=$validServerUri")

        context("and a Kotlin project") {
            val project = initDokkatooProject()

            context("when generate task is run with correct server URI") {
                project.runner
                    .addArguments(
                        "clean",
                        "dokkatooGeneratePublicationHtml",
                        "--stacktrace",
                        "--info",
                        validServerUriParam,
                    )
                    .forwardOutput()
                    .build {
                        test("expect project builds successfully") {
                            output shouldContain "BUILD SUCCESSFUL"
                        }
                        test("LogHtmlPublicationLinkTask should run") {
                            shouldHaveTasksWithAnyOutcome(
                                ":logLinkDokkatooGeneratePublicationHtml" to listOf(SUCCESS)
                            )
                        }
                        test("expect link is logged") {
                            output shouldContain "Generated Dokka HTML publication: $validServerUri/log-html-publication-link-task/build/dokka/html/index.html"
                        }
                    }
            }

            context("and the server is down") {
                // stop the server immediately
                server.stop(gracePeriodMillis = 0, timeoutMillis = 0)

                context("when running the generate task") {
                    project.runner
                        .addArguments(
                            "clean",
                            "dokkatooGeneratePublicationHtml",
                            "--stacktrace",
                            "--info",
                            validServerUriParam,
                        )
                        .forwardOutput()
                        .build {
                            test("expect project builds successfully") {
                                output shouldContain "BUILD SUCCESSFUL"
                            }
                            test("LogHtmlPublicationLinkTask should be skipped") {
                                shouldHaveTasksWithAnyOutcome(
                                    ":logLinkDokkatooGeneratePublicationHtml" to listOf(SKIPPED)
                                )
                                output shouldContain "Skipping task ':logLinkDokkatooGeneratePublicationHtml' as task onlyIf 'server URL is reachable' is false"
                            }
                            test("expect link is not logged") {
                                output shouldNotContain "Generated Dokka HTML publication"
                            }
                        }
                }
            }
        }
    }
}) {
    companion object {
        @Suppress("SpellCheckingInspection")
        /**
         * prefix [param] with `-P`.
         *
         * (this exists to avoid annoying typo warnings, e.g. `-Pserver=localhost` -> `Typo: In word 'Pserver'`)
         */
        private fun `-P`(param: String): String = "-P$param"
    }
}


private fun initDokkatooProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest("log-html-publication-link-task") {
        buildGradleKts = """
            |plugins {
            |  kotlin("jvm") version "1.8.22"
            |  id("org.jetbrains.dokka") version "${DokkatooConstants.DOKKATOO_VERSION}"
            |}
            |
            |dependencies {
            |}
            |
            |tasks.withType<org.jetbrains.dokka.gradle.tasks.LogHtmlPublicationLinkTask>().configureEach {
            |  serverUri.set(providers.gradleProperty("testServerUri"))
            |}
            """.trimMargin()

        createKotlinFile(
            "src/main/kotlin/Hello.kt",
            """
            |package com.project.hello
            |
            |/** The Hello class */
            |class Hello {
            |    /** prints `Hello` to the console */  
            |    fun sayHello() = println("Hello")
            |}
            |
            """.trimMargin()
        )

        // remove the flag that disables the logging task, since this test wants the logger to run
        defaultRunnerArgs.removeIf { ENABLE_TASK_PROPERTY_NAME in it }

        config()
    }
}
