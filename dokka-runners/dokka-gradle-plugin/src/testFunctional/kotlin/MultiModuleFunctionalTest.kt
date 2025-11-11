/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.sequences.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.dokka.gradle.WorkerIsolation.ClassLoader
import org.jetbrains.dokka.gradle.WorkerIsolation.Process
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject
import kotlin.io.path.*

class MultiModuleFunctionalTest : FunSpec({

    context("when DGP generates all formats") {
        val project = initMultiModuleProject("all-formats")

        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--stacktrace",
                "--rerun-tasks",
            )
            .forwardOutput()
            .build {
                test("expect build is successful") {
                    output shouldContain "BUILD SUCCESSFUL"
                }

                test("expect all Dokka workers are successful") {
                    project
                        .findFiles { it.name == "dokka-worker.log" }
                        .shouldForAll { dokkaWorkerLog ->
                            dokkaWorkerLog.shouldBeAFile()
                            dokkaWorkerLog.readText().shouldNotContainAnyOf(
                                "[ERROR]",
                                "[WARN]",
                            )
                        }
                }

                context("expect HTML site is generated") {

                    test("with expected HTML files") {

                        project.projectDir
                            .resolve("build/dokka/")
                            .listRelativePathsMatching { it.extension == "html" }
                            .shouldContainExactlyInAnyOrder(
                                "html/index.html",
                                "html/navigation.html",
                                "html/subproject-goodbye/com.project.goodbye/-goodbye/-goodbye.html",
                                "html/subproject-goodbye/com.project.goodbye/-goodbye/index.html",
                                "html/subproject-goodbye/com.project.goodbye/-goodbye/say-hello.html",
                                "html/subproject-goodbye/com.project.goodbye/index.html",
                                "html/subproject-goodbye/index.html",
                                "html/subproject-goodbye/navigation.html",
                                "html/subproject-hello/com.project.hello/-hello/-hello.html",
                                "html/subproject-hello/com.project.hello/-hello/index.html",
                                "html/subproject-hello/com.project.hello/-hello/say-hello.html",
                                "html/subproject-hello/com.project.hello/index.html",
                                "html/subproject-hello/index.html",
                                "html/subproject-hello/navigation.html",
                            )
                    }

                    test("with element-list") {
                        project.file("build/dokka/html/package-list").toFile().shouldBeAFile()
                        project.file("build/dokka/html/package-list").readText()
                            .lines()
                            .sorted()
                            .joinToString("\n")
                            .shouldContain( /* language=text */ """
                                |${'$'}dokka.format:html-v1
                                |${'$'}dokka.linkExtension:html
                                |com.project.goodbye
                                |com.project.hello
                                |module:subproject-goodbye
                                |module:subproject-hello
                                """.trimMargin()
                            )
                    }
                }
            }
    }

    context("Gradle caching") {

        context("expect DGP is compatible with Gradle Build Cache") {
            val project = initMultiModuleProject("build-cache")

            test("expect clean is successful") {
                project.runner.addArguments("clean").build {
                    output shouldContain "BUILD SUCCESSFUL"
                }
            }

            project.runner
                .addArguments(
                    ":dokkaGenerate",
                    "--stacktrace",
                    "--build-cache",
                )
                .forwardOutput()
                .build {
                    test("expect build is successful") {
                        output shouldContain "BUILD SUCCESSFUL"
                    }

                    test("expect all Dokka workers are successful") {
                        project
                            .findFiles { it.name == "dokka-worker.log" }
                            .shouldForAll { dokkaWorkerLog ->
                                dokkaWorkerLog.shouldBeAFile()
                                dokkaWorkerLog.readText().shouldNotContainAnyOf(
                                    "[ERROR]",
                                    "[WARN]",
                                )
                            }
                    }
                }

            context("when build cache is enabled") {
                project.runner
                    .addArguments(
                        ":dokkaGenerate",
                        "--stacktrace",
                        "--build-cache",
                    )
                    .forwardOutput()
                    .build {
                        test("expect build is successful") {
                            output shouldContainAll listOf(
                                "BUILD SUCCESSFUL",
                                "6 actionable tasks: 6 up-to-date",
                            )
                        }

                        test("expect all Dokka tasks are up-to-date") {
                            tasks
                                .filter { task ->
                                    task.name.contains("dokka", ignoreCase = true)
                                }
                                .shouldNotBeEmpty()
                                .shouldForAll { task ->
                                    task.outcome.shouldBeIn(FROM_CACHE, UP_TO_DATE, SKIPPED)
                                }
                        }
                    }
            }
        }

        context("build cache relocation") {

            val originalProject = initMultiModuleProject("build-cache-relocation/original/") {
                buildGradleKts += """
                    dokka {
                        moduleName.set("demo-project")
                    }
                    """.trimIndent()
            }
            // Create the _same_ project in a different dir, to verify that the build cache
            // can be re-used and doesn't have path-sensitive inputs/outputs.
            val relocatedProject = initMultiModuleProject("build-cache-relocation/relocated/project/") {
                buildGradleKts += """
                    dokka {
                        moduleName.set("demo-project")
                    }
                    """.trimIndent()
            }

            // create custom build cache dir, so it's easier to control, specify, and clean-up
            val buildCacheDir = originalProject.projectDir.resolve("build-cache")

            val expectedGenerationTasks = listOf(
                ":dokkaGeneratePublicationHtml",
                ":subproject-hello:dokkaGenerateModuleHtml",
                ":subproject-goodbye:dokkaGenerateModuleHtml",
                ":dokkaGeneratePublicationJavadoc",
                ":subproject-hello:dokkaGenerateModuleJavadoc",
                ":subproject-goodbye:dokkaGenerateModuleJavadoc",
            )

            test("setup build cache") {
                buildCacheDir.deleteRecursively()
                buildCacheDir.createDirectories()

                val buildCacheConfig = """
                    |
                    |buildCache { 
                    |  local { 
                    |    directory = "${buildCacheDir.invariantSeparatorsPathString}"
                    |  }
                    |}
                    |
                    """.trimMargin()

                originalProject.settingsGradleKts += buildCacheConfig
                relocatedProject.settingsGradleKts += buildCacheConfig
            }

            context("original project") {
                test("clean tasks should run successfully") {
                    originalProject.runner
                        .addArguments(
                            "clean",
                            "--build-cache",
                            "--stacktrace",
                        )
                        .forwardOutput()
                        .build {
                            shouldHaveTasksWithAnyOutcome(
                                ":clean" to listOf(UP_TO_DATE, SUCCESS),
                                ":subproject-hello:clean" to listOf(UP_TO_DATE, SUCCESS),
                                ":subproject-goodbye:clean" to listOf(UP_TO_DATE, SUCCESS),
                            )

                            output.shouldContain("BUILD SUCCESSFUL")
                        }
                }
                test("should execute all generation tasks") {
                    originalProject.runner
                        .addArguments(
                            ":dokkaGenerate",
                            "--stacktrace",
                            "--build-cache",
                            "-D" + "org.gradle.caching.debug=true",
                        )
                        .forwardOutput()
                        .build {
                            shouldHaveTasksWithOutcome(expectedGenerationTasks.map { it to SUCCESS })
                        }
                }
            }

            context("relocated project") {
                test("clean tasks should run successfully") {
                    relocatedProject.runner
                        .addArguments(
                            "clean",
                            "--build-cache",
                            "--stacktrace",
                        )
                        .forwardOutput()
                        .build {
                            shouldHaveTasksWithAnyOutcome(
                                ":clean" to listOf(UP_TO_DATE, SUCCESS),
                                ":subproject-hello:clean" to listOf(UP_TO_DATE, SUCCESS),
                                ":subproject-goodbye:clean" to listOf(UP_TO_DATE, SUCCESS),
                            )
                        }
                }

                test("should load all generation tasks from cache") {
                    relocatedProject.runner
                        .addArguments(
                            ":dokkaGenerate",
                            "--stacktrace",
                            "--build-cache",
                            "-D" + "org.gradle.caching.debug=true",
                        )
                        .forwardOutput()
                        .build {
                            shouldHaveTasksWithOutcome(expectedGenerationTasks.map { it to FROM_CACHE })
                        }
                }
            }
        }


        context("Gradle Configuration Cache") {
            val project = initMultiModuleProject("config-cache")

            test("expect clean is successful") {
                project.runner.addArguments("clean").build {
                    output shouldContain "BUILD SUCCESSFUL"
                }
            }

            project.runner
                .addArguments(
                    //"clean",
                    ":dokkaGenerate",
                    "--stacktrace",
                    "--no-build-cache",
                    "--configuration-cache",
                )
                .forwardOutput()
                .build {
                    test("expect build is successful") {
                        output shouldContain "BUILD SUCCESSFUL"
                    }
                }

            test("expect all Dokka workers are successful") {
                project
                    .findFiles { it.name == "dokka-worker.log" }
                    .shouldForAll { dokkaWorkerLog ->
                        dokkaWorkerLog.shouldBeAFile()
                        dokkaWorkerLog.readText().shouldNotContainAnyOf(
                            "[ERROR]",
                            "[WARN]",
                        )
                    }
            }
        }


        context("expect updates in subprojects re-run tasks") {

            val project = initMultiModuleProject("submodule-update")

            test("expect clean is successful") {
                project.runner.addArguments("clean").build {
                    output shouldContain "BUILD SUCCESSFUL"
                }
            }

            test("expect first build is successful") {
                project.runner
                    .addArguments(
                        //"clean",
                        ":dokkaGeneratePublicationHtml",
                        "--stacktrace",
                        "--build-cache",
                    )
                    .forwardOutput()
                    .build {
                        output shouldContain "BUILD SUCCESSFUL"
                    }
            }

            context("and when a file in a subproject changes") {

                val helloAgainIndexHtml =
                    @Suppress("KDocUnresolvedReference")
                    project.createKotlinFile(
                        "subproject-hello/src/main/kotlin/HelloAgain.kt",
                        """
                        |package com.project.hello
                        |
                        |/** Like [Hello], but again */
                        |class HelloAgain {
                        |    /** prints `Hello Again` to the console */  
                        |    fun sayHelloAgain() = println("Hello Again")
                        |}
                        |
                        """.trimMargin()
                    ).toPath()

                context("expect Dokka re-generates the publication") {
                    project.runner
                        .addArguments(
                            ":dokkaGeneratePublicationHtml",
                            "--stacktrace",
                            "--build-cache",
                        )
                        .forwardOutput()
                        .build {

                            test("expect HelloAgain HTML file exists") {
                                // convert to file, workaround https://github.com/kotest/kotest/issues/3825
                                helloAgainIndexHtml.toFile().shouldBeAFile()
                            }

                            test("expect :subproject-goodbye tasks are up-to-date, because no files changed") {
                                shouldHaveTasksWithOutcome(
                                    ":subproject-goodbye:dokkaGenerateModuleHtml" to UP_TO_DATE,
                                )
                            }

                            val successfulOutcomes = listOf(SUCCESS, FROM_CACHE)
                            test("expect :subproject-hello tasks should be re-run, since a file changed") {
                                shouldHaveTasksWithAnyOutcome(
                                    ":subproject-hello:dokkaGenerateModuleHtml" to successfulOutcomes,
                                )
                            }

                            test("expect aggregating tasks should re-run because the :subproject-hello Dokka Module changed") {
                                shouldHaveTasksWithAnyOutcome(
                                    ":dokkaGeneratePublicationHtml" to successfulOutcomes,
                                )
                            }

                            test("expect build is successful") {
                                output shouldContain "BUILD SUCCESSFUL"
                            }

                            test("expect 3 actionable tasks") {
                                output shouldContain "3 actionable tasks"
                            }
                        }

                    context("and when the class is deleted") {
                        project.dir("subproject-hello") {
                            require(file("src/main/kotlin/HelloAgain.kt").toFile().delete()) {
                                "failed to delete HelloAgain.kt"
                            }
                        }

                        project.runner
                            .addArguments(
                                ":dokkaGeneratePublicationHtml",
                                "--stacktrace",
                                "--info",
                                "--build-cache",
                            )
                            .forwardOutput()
                            .build {

                                test("expect HelloAgain HTML file is now deleted") {
                                    helloAgainIndexHtml.shouldNotExist()

                                    project.dir("build/dokka/html/") {
                                        projectDir.toTreeString().shouldNotContainAnyOf(
                                            "hello-again",
                                            "-hello-again/",
                                            "-hello-again.html",
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    context("logging") {
        val project = initMultiModuleProject("logging")

        test("expect no logs when built using --quiet log level") {
            project.runner
                .addArguments(
                    "clean",
                    ":dokkaGenerate",
                    "--no-configuration-cache",
                    "--no-build-cache",
                    "--quiet",
                    "--stacktrace",
                )
                .forwardOutput()
                .build {
                    output.shouldBeEmpty()
                }
        }

        test("expect no Dokka Gradle Plugin logs when built using lifecycle log level") {
            project.runner
                .addArguments(
                    "clean",
                    ":dokkaGenerate",
                    "--no-configuration-cache",
                    "--no-build-cache",
                    "--no-parallel",
                    "--stacktrace",
                    // no logging option => lifecycle log level
                )
                .forwardOutput()
                .build {

                    // projects are only configured the first time TestKit runs, and annoyingly there's no
                    // easy way to force Gradle to re-configure the projects - so only check conditionally.
                    if ("Configure project" in output) {
                        output shouldContain /*language=text*/ """
                            ¦> Configure project :
                            ¦> Configure project :subproject-goodbye
                            ¦> Configure project :subproject-hello
                            ¦> Task :clean
                            """.trimMargin("¦")
                    }

                    output.lines()
                        .filter { it.startsWith("> Task :") }
                        .shouldContainAll(
                            "> Task :clean",
                            "> Task :subproject-goodbye:clean",
                            "> Task :subproject-hello:clean",

                            "> Task :dokkaGenerate",

                            "> Task :dokkaGeneratePublicationHtml",
                            "> Task :dokkaGeneratePublicationJavadoc",

                            "> Task :subproject-hello:dokkaGenerateModuleHtml",
                            "> Task :subproject-hello:dokkaGenerateModuleJavadoc",

                            "> Task :subproject-goodbye:dokkaGenerateModuleHtml",
                            "> Task :subproject-goodbye:dokkaGenerateModuleJavadoc",
                        )
                }
        }
    }

    context("KotlinProjectExtension failure warning") {
        val project = initMultiModuleProject("kpe-warning") {
            buildGradleKts = buildGradleKts.lines().joinToString("\n") { line ->
                when {
                    line.startsWith("""  kotlin("jvm")""") -> "//$line"

                    else -> line
                }
            }
        }

        test("expect KotlinAdapter not applied to root project") {
            project.runner
                .addArguments(
                    "clean",
                    "--stacktrace",
                    "--info",
                )
                .build {
                    // the root project doesn't have KGP applied, so KotlinAdapter shouldn't be applied
                    output shouldContain "Dokka Gradle Plugin could not load KotlinBasePlugin in root project 'kpe-warning'"
                    output shouldNotContain "Applying KotlinAdapter to :\n"
                }
        }

        test("expect KotlinAdapter applied to subprojects, with KotlinProjectExtension warnings") {
            project.runner
                .addArguments(
                    "clean",
                    "--stacktrace",
                    "--warn",
                )
                .build {
                    // the subprojects should have KotlinAdapter applied, but the extension should be unavailable
                    // because the buildscript classpath is inconsistent.
                    // (DGP is applied to the root project, but KGP is not.)
                    output shouldContain "Dokka Gradle Plugin could not load KotlinBasePlugin in project ':subproject-hello', but plugin org.jetbrains.kotlin.jvm is applied"
                    output shouldContain "Dokka Gradle Plugin could not load KotlinBasePlugin in project ':subproject-goodbye', but plugin org.jetbrains.kotlin.jvm is applied"
                }
        }
    }

    WorkerIsolation.values().forEach { isolation ->
        context("DokkaGenerateTask worker $isolation") {

            val project = initMultiModuleProject("worker-$isolation") {
                val workerIsolationConfig = when (isolation) {
                    ClassLoader -> { // language=kts
                        """
                        |
                        |dokka {
                        |  dokkaGeneratorIsolation.set(
                        |    ClassLoaderIsolation()
                        |  )
                        |}
                        |
                        """.trimMargin()
                    }

                    Process ->  // language=kts
                        """
                        |
                        |dokka {
                        |  dokkaGeneratorIsolation.set(
                        |    ProcessIsolation {
                        |       debug.set(false)
                        |       enableAssertions.set(true)
                        |       minHeapSize.set("64m")
                        |       maxHeapSize.set("512m")
                        |       jvmArgs.set(listOf("-XX:+HeapDumpOnOutOfMemoryError"))
                        |       //allJvmArgs.set(listOf("b"))
                        |       defaultCharacterEncoding.set("UTF-8")
                        |       systemProperties.set(mapOf("a" to "b"))
                        |     }
                        |  )
                        |}
                        |
                        """.trimMargin()
                }

                // language=kts
                val tasksLogWorkerIsolation = """
                    |
                    |tasks.withType<org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask>().configureEach {
                    |  doFirst {
                    |    val isolation = workerIsolation.get()
                    |    logger.lifecycle(path + " - running with workerIsolation " + isolation)
                    |  }
                    |}
                    |
                    """.trimMargin()

                buildGradleKts += workerIsolationConfig + tasksLogWorkerIsolation
                dir("subproject-hello") {
                    buildGradleKts += workerIsolationConfig + tasksLogWorkerIsolation
                }
                dir("subproject-goodbye") {
                    buildGradleKts += workerIsolationConfig + tasksLogWorkerIsolation
                }
            }

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun-tasks",
                    "--stacktrace",
                )
                .forwardOutput()
                .build {

                    test("expect project builds successfully") {
                        output shouldContain "BUILD SUCCESSFUL"
                    }

                    val expectedIsolationClassFqn = when (isolation) {
                        ClassLoader -> "WorkerIsolation.ClassLoader"
                        Process -> "WorkerIsolation.Process"
                    }

                    listOf(
                        ":subproject-goodbye:dokkaGenerateModuleHtml",
                        ":subproject-hello:dokkaGenerateModuleHtml",
                        ":dokkaGeneratePublicationHtml",
                    ).forEach { dokkaTaskPath ->
                        test("expect $dokkaTaskPath runs with isolation $isolation") {
                            output shouldContain "$dokkaTaskPath - running with workerIsolation $expectedIsolationClassFqn"
                        }
                    }

                    test("expect 3 tasks log worker isolation mode") {
                        withClue(output) {
                            output.lines().count { "running with workerIsolation" in it } shouldBe 3
                        }
                    }

                    test("expect no 'unknown class' message in HTML files") {
                        val htmlFiles = project.projectDir.toFile()
                            .resolve("build/dokka/html")
                            .walk()
                            .filter { it.isFile && it.extension == "html" }

                        withClue("html files should be generated") {
                            htmlFiles.shouldNotBeEmpty()
                        }

                        htmlFiles.forEach { htmlFile ->
                            val relativePath = htmlFile.relativeTo(project.projectDir.toFile())
                            withClue("$relativePath should not contain Error class: unknown class") {
                                htmlFile.useLines { lines ->
                                    lines.shouldForAll { line -> line.shouldNotContain("Error class: unknown class") }
                                }
                            }
                        }
                    }
                }
        }
    }
})


private enum class WorkerIsolation { ClassLoader, Process }
