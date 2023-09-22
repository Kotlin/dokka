package org.jetbrains.dokka.dokkatoo

import org.jetbrains.dokka.dokkatoo.internal.DokkatooConstants.DOKKATOO_VERSION
import org.jetbrains.dokka.dokkatoo.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome.*

class MultiModuleFunctionalTest : FunSpec({

  context("when dokkatoo generates all formats") {
    val project = initDokkatooProject("all-formats")

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
        project.file("subproject/build/dokka/html/index.html").shouldBeAFile()
        project.file("subproject/build/dokka/html/com/project/hello/Hello.html")
          .shouldBeAFile()
      }

      test("and dokka_parameters.json is generated") {
        project.file("subproject/build/dokka/html/dokka_parameters.json")
          .shouldBeAFile()
      }

      test("with element-list") {
        project.file("build/dokka/html/package-list").shouldBeAFile()
        project.file("build/dokka/html/package-list").toFile().readText()
          .shouldContain( /* language=text */ """
              |${'$'}dokka.format:html-v1
              |${'$'}dokka.linkExtension:html
              |
              |module:subproject-hello
              |com.project.hello
              |module:subproject-goodbye
              |com.project.goodbye
            """.trimMargin()
          )
      }
    }
  }

  context("Gradle caching") {

    context("expect Dokkatoo is compatible with Gradle Build Cache") {
      val project = initDokkatooProject("build-cache")

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
                "24 actionable tasks: 24 up-to-date",
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

    context("Gradle Configuration Cache") {
      val project = initDokkatooProject("config-cache")

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

      val project = initDokkatooProject("submodule-update")

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
                helloAgainIndexHtml.shouldBeAFile()
              }

              test("expect :subproject-goodbye tasks are up-to-date, because no files changed") {
                shouldHaveTasksWithOutcome(
                  ":subproject-goodbye:dokkatooGenerateModuleHtml" to UP_TO_DATE,
                  ":subproject-goodbye:prepareDokkatooModuleDescriptorHtml" to UP_TO_DATE,
                )
              }

              val successfulOutcomes = listOf(SUCCESS, FROM_CACHE)
              test("expect :subproject-hello tasks should be re-run, since a file changed") {
                shouldHaveTasksWithAnyOutcome(
                  ":subproject-hello:dokkatooGenerateModuleHtml" to successfulOutcomes,
                  ":subproject-hello:prepareDokkatooModuleDescriptorHtml" to successfulOutcomes,
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

              test("expect 5 tasks are run") {
                output shouldContain "5 actionable tasks"
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
    val project = initDokkatooProject("logging")

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
              "> Task :subproject-goodbye:prepareDokkatooModuleDescriptorGfm",
              "> Task :subproject-goodbye:prepareDokkatooModuleDescriptorHtml",
              "> Task :subproject-goodbye:prepareDokkatooModuleDescriptorJavadoc",
              "> Task :subproject-goodbye:prepareDokkatooModuleDescriptorJekyll",
              "> Task :subproject-hello:clean",
              "> Task :subproject-hello:dokkatooGenerateModuleGfm",
              "> Task :subproject-hello:dokkatooGenerateModuleHtml",
              "> Task :subproject-hello:dokkatooGenerateModuleJavadoc",
              "> Task :subproject-hello:dokkatooGenerateModuleJekyll",
              "> Task :subproject-hello:prepareDokkatooModuleDescriptorGfm",
              "> Task :subproject-hello:prepareDokkatooModuleDescriptorHtml",
              "> Task :subproject-hello:prepareDokkatooModuleDescriptorJavadoc",
              "> Task :subproject-hello:prepareDokkatooModuleDescriptorJekyll",
            )
        }
    }
  }
})

private fun initDokkatooProject(
  testName: String,
  config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
  return gradleKtsProjectTest("multi-module-hello-goodbye/$testName") {

    settingsGradleKts += """
      |
      |include(":subproject-hello")
      |include(":subproject-goodbye")
      |
    """.trimMargin()

    buildGradleKts = """
      |plugins {
      |  // Kotlin plugin shouldn't be necessary here, but without it Dokka errors
      |  // with ClassNotFound KotlinPluginExtension... very weird
      |  kotlin("jvm") version "1.8.22" apply false
      |  id("org.jetbrains.dokka.dokkatoo") version "$DOKKATOO_VERSION"
      |}
      |
      |dependencies {
      |  dokkatoo(project(":subproject-hello"))
      |  dokkatoo(project(":subproject-goodbye"))
      |  dokkatooPluginHtml(
      |    dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
      |      "org.jetbrains.dokka:all-modules-page-plugin:${'$'}dokkaVersion"
      |    }
      |  )
      |}
      |
    """.trimMargin()

    dir("subproject-hello") {
      buildGradleKts = """
          |plugins {
          |  kotlin("jvm") version "1.8.22"
          |  id("org.jetbrains.dokka.dokkatoo") version "$DOKKATOO_VERSION"
          |}
          |
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

      createKotlinFile("src/main/kotlin/HelloAgain.kt", "")
    }

    dir("subproject-goodbye") {

      buildGradleKts = """
          |plugins {
          |  kotlin("jvm") version "1.8.22"
          |  id("org.jetbrains.dokka.dokkatoo") version "$DOKKATOO_VERSION"
          |}
          |
        """.trimMargin()

      createKotlinFile(
        "src/main/kotlin/Goodbye.kt",
        """
          |package com.project.goodbye
          |
          |/** The Goodbye class */
          |class Goodbye {
          |    /** prints a goodbye message to the console */  
          |    fun sayHello() = println("Goodbye!")
          |}
          |
        """.trimMargin()
      )
    }

    config()
  }
}
