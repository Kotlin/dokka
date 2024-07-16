package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.WorkerIsolation.ClassLoader
import dev.adamko.dokkatoo.WorkerIsolation.Process
import dev.adamko.dokkatoo.utils.*
import dev.adamko.dokkatoo.utils.projects.initMultiModuleProject
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.sequences.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.io.path.deleteRecursively
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText
import org.gradle.testkit.runner.TaskOutcome.*

class MultiModuleFunctionalTest : FunSpec({

  context("when dokkatoo generates all formats") {
    val project = initMultiModuleProject("all-formats")

    project.runner
      .addArguments(
        "clean",
        ":dokkatooGenerate",
        "--stacktrace",
      )
      .forwardOutput()
      .build {
        test("expect build is successful") {
          output shouldContain "BUILD SUCCESSFUL"
        }

        test("expect all dokka workers are successful") {
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

    context("expect Dokkatoo is compatible with Gradle Build Cache") {
      val project = initMultiModuleProject("build-cache")

      test("expect clean is successful") {
        project.runner.addArguments("clean").build {
          output shouldContain "BUILD SUCCESSFUL"
        }
      }

      project.runner
        .addArguments(
          //"clean",
          ":dokkatooGenerate",
          "--stacktrace",
          "--build-cache",
        )
        .forwardOutput()
        .build {
          test("expect build is successful") {
            output shouldContain "BUILD SUCCESSFUL"
          }

          test("expect all dokka workers are successful") {
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
            ":dokkatooGenerate",
            "--stacktrace",
            "--build-cache",
          )
          .forwardOutput()
          .build {
            test("expect build is successful") {
              output shouldContainAll listOf(
                "BUILD SUCCESSFUL",
                "16 actionable tasks: 16 up-to-date",
              )
            }

            test("expect all dokkatoo tasks are up-to-date") {
              tasks
                .filter { task ->
                  task.name.contains("dokkatoo", ignoreCase = true)
                }
                .shouldForAll { task ->
                  task.outcome.shouldBeIn(FROM_CACHE, UP_TO_DATE, SKIPPED)
                }
            }
          }
      }
    }

    context("build cache relocation") {

      val originalProject = initMultiModuleProject("build-cache-relocation/original/")
      // Create the _same_ project in a different dir, to verify that the build cache
      // can be re-used and doesn't have path-sensitive inputs/outputs.
      val relocatedProject = initMultiModuleProject("build-cache-relocation/relocated/project/")

      // create custom build cache dir, so it's easier to control, specify, and clean-up
      val buildCacheDir = originalProject.projectDir.resolve("build-cache")

      val expectedGenerationTasks = listOf(
        ":dokkatooGeneratePublicationGfm",
        ":dokkatooGeneratePublicationHtml",
        ":dokkatooGeneratePublicationJavadoc",
        ":dokkatooGeneratePublicationJekyll",
        ":subproject-hello:dokkatooGenerateModuleGfm",
        ":subproject-hello:dokkatooGenerateModuleHtml",
        ":subproject-hello:dokkatooGenerateModuleJavadoc",
        ":subproject-hello:dokkatooGenerateModuleJekyll",
        ":subproject-goodbye:dokkatooGenerateModuleGfm",
        ":subproject-goodbye:dokkatooGenerateModuleHtml",
        ":subproject-goodbye:dokkatooGenerateModuleJavadoc",
        ":subproject-goodbye:dokkatooGenerateModuleJekyll",
      )

      test("setup build cache") {
        buildCacheDir.deleteRecursively()
        buildCacheDir.toFile().mkdirs()

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
        originalProject.runner
          .addArguments(
            "clean",
            "--build-cache",
          )
          .forwardOutput()
          .build {
            test("clean tasks should run successfully") {
              shouldHaveTasksWithAnyOutcome(
                ":clean" to listOf(UP_TO_DATE, SUCCESS),
                ":subproject-hello:clean" to listOf(UP_TO_DATE, SUCCESS),
                ":subproject-goodbye:clean" to listOf(UP_TO_DATE, SUCCESS),
              )

              output.shouldContain("BUILD SUCCESSFUL")
            }
          }
        originalProject.runner
          .addArguments(
            ":dokkatooGenerate",
            "--stacktrace",
            "--build-cache",
            "-D" + "org.gradle.caching.debug=true"
          )
          .forwardOutput()
          .build {
            test("should execute all generation tasks") {
              shouldHaveTasksWithOutcome(expectedGenerationTasks.map { it to SUCCESS })
            }
          }
      }

      context("relocated project") {
        relocatedProject.runner
          .addArguments(
            "clean",
            "--build-cache",
          )
          .forwardOutput()
          .build {
            test("clean tasks should run successfully") {
              shouldHaveTasksWithAnyOutcome(
                ":clean" to listOf(UP_TO_DATE, SUCCESS),
                ":subproject-hello:clean" to listOf(UP_TO_DATE, SUCCESS),
                ":subproject-goodbye:clean" to listOf(UP_TO_DATE, SUCCESS),
              )
            }
          }

        relocatedProject.runner
          .addArguments(
            ":dokkatooGenerate",
            "--stacktrace",
            "--build-cache",
            "-D" + "org.gradle.caching.debug=true"
          )
          .forwardOutput()
          .build {
            test("should load all generation tasks from cache") {
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
          ":dokkatooGenerate",
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

      test("expect all dokka workers are successful") {
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
            ":dokkatooGeneratePublicationHtml",
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
              ":dokkatooGeneratePublicationHtml",
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
                  ":subproject-goodbye:dokkatooGenerateModuleHtml" to UP_TO_DATE,
                )
              }

              val successfulOutcomes = listOf(SUCCESS, FROM_CACHE)
              test("expect :subproject-hello tasks should be re-run, since a file changed") {
                shouldHaveTasksWithAnyOutcome(
                  ":subproject-hello:dokkatooGenerateModuleHtml" to successfulOutcomes,
                )
              }

              test("expect aggregating tasks should re-run because the :subproject-hello Dokka Module changed") {
                shouldHaveTasksWithAnyOutcome(
                  ":dokkatooGeneratePublicationHtml" to successfulOutcomes,
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
                ":dokkatooGeneratePublicationHtml",
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
          ":dokkatooGenerate",
          "--no-configuration-cache",
          "--no-build-cache",
          "--quiet",
        )
        .forwardOutput()
        .build {
          output.shouldBeEmpty()
        }
    }

    test("expect no Dokkatoo logs when built using lifecycle log level") {
      project.runner
        .addArguments(
          "clean",
          ":dokkatooGenerate",
          "--no-configuration-cache",
          "--no-build-cache",
          "--no-parallel",
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
              "> Task :dokkatooGenerate",
              "> Task :dokkatooGenerateModuleGfm",
              "> Task :dokkatooGenerateModuleHtml",
              "> Task :dokkatooGenerateModuleJavadoc",
              "> Task :dokkatooGenerateModuleJekyll",
              "> Task :dokkatooGeneratePublicationGfm",
              "> Task :dokkatooGeneratePublicationHtml",
              "> Task :dokkatooGeneratePublicationJavadoc",
              "> Task :dokkatooGeneratePublicationJekyll",
              "> Task :subproject-goodbye:clean",
              "> Task :subproject-goodbye:dokkatooGenerateModuleGfm",
              "> Task :subproject-goodbye:dokkatooGenerateModuleHtml",
              "> Task :subproject-goodbye:dokkatooGenerateModuleJavadoc",
              "> Task :subproject-goodbye:dokkatooGenerateModuleJekyll",
              "> Task :subproject-hello:clean",
              "> Task :subproject-hello:dokkatooGenerateModuleGfm",
              "> Task :subproject-hello:dokkatooGenerateModuleHtml",
              "> Task :subproject-hello:dokkatooGenerateModuleJavadoc",
              "> Task :subproject-hello:dokkatooGenerateModuleJekyll",
            )
        }
    }
  }

  context("KotlinProjectExtension failure warning") {
    val project = initMultiModuleProject("kpe-warning") {
      buildGradleKts = buildGradleKts.lines().joinToString("\n") { line ->
        when {
          line.startsWith("""  kotlin("jvm")""") -> "//$line"

          else                                   -> line
        }
      }
    }

    test("expect warning regarding KotlinProjectExtension") {
      project.runner
        .addArguments("clean")
        .forwardOutput()
        .build {
          // the root project doesn't have the KGP applied, so KotlinProjectExtension shouldn't be applied
          output shouldNotContain "DokkatooKotlinAdapter failed to get KotlinProjectExtension in :\n"

          output shouldContain "DokkatooKotlinAdapter failed to get KotlinProjectExtension in :subproject-hello\n"
          output shouldContain "DokkatooKotlinAdapter failed to get KotlinProjectExtension in :subproject-goodbye\n"
        }
    }
  }

  WorkerIsolation.values().forEach { isolation ->
    context("DokkatooGenerateTask worker $isolation") {

      val project = initMultiModuleProject("worker-$isolation") {
        val workerIsolationConfig = when (isolation) {
          ClassLoader -> { // language=kts
            """
              |
              |dokkatoo {
              |  dokkaGeneratorIsolation.set(
              |    ClassLoaderIsolation()
              |  )
              |}
              |
            """.trimMargin()
          }

          Process     ->  // language=kts
            """
              |
              |dokkatoo {
              |  dokkaGeneratorIsolation.set(
              |    ProcessIsolation {
              |       debug.set(false)
              |       enableAssertions.set(true)
              |       minHeapSize.set("64m")
              |       maxHeapSize.set("512m")
              |       jvmArgs.set(listOf("a"))
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
          |tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
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
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
        )
        .forwardOutput()
        .build {

          test("expect project builds successfully") {
            output shouldContain "BUILD SUCCESSFUL"
          }

          val expectedIsolationClassFqn = when (isolation) {
            ClassLoader -> "dev.adamko.dokkatoo.workers.ClassLoaderIsolation"
            Process     -> "dev.adamko.dokkatoo.workers.ProcessIsolation"
          }

          listOf(
            ":subproject-goodbye:dokkatooGenerateModuleHtml",
            ":subproject-hello:dokkatooGenerateModuleHtml",
            ":dokkatooGeneratePublicationHtml",
          ).forEach { dokkatooTaskPath ->
            test("expect $dokkatooTaskPath runs with isolation $isolation") {
              output shouldContain "$dokkatooTaskPath - running with workerIsolation $expectedIsolationClassFqn"
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
