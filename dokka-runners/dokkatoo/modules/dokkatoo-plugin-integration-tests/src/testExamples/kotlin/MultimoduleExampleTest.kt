package org.jetbrains.dokka.dokkatoo.tests.examples

import org.jetbrains.dokka.dokkatoo.internal.DokkatooConstants.DOKKA_VERSION
import org.jetbrains.dokka.dokkatoo.utils.*
import org.jetbrains.dokka.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class MultimoduleExampleTest : FunSpec({

  val dokkaProject = initDokkaProject(
    projectTestTempDir.resolve("it/examples/multimodule-example/dokka").toFile()
  )

  val dokkatooProject = initDokkatooProject(
    projectTestTempDir.resolve("it/examples/multimodule-example/dokkatoo").toFile()
  )

  context("compare dokka and dokkatoo HTML generators") {
    test("expect dokka can generate HTML") {
      dokkaProject.runner
        .addArguments(
          "clean",
          "dokkaHtmlMultiModule",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build {
          output shouldContain "BUILD SUCCESSFUL"
          output shouldContain "Generation completed successfully"
        }
    }

    context("when Dokkatoo generates HTML") {
      dokkatooProject.runner
        .addArguments(
          "clean",
          ":parentProject:dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build {
          test("expect build is successful") {
            output shouldContain "BUILD SUCCESSFUL"
          }

          test("expect all dokka workers are successful") {
            dokkatooProject
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
    }

    context("expect dokka and dokkatoo HTML is the same") {
      val dokkaHtmlDir =
        dokkaProject.projectDir.resolve("parentProject/build/dokka/html")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("parentProject/build/dokka/html")

      test("expect file trees are the same") {
        val expectedFileTree = dokkaHtmlDir.toTreeString()
        val actualFileTree = dokkatooHtmlDir.toTreeString()
        println((actualFileTree to expectedFileTree).sideBySide())
        expectedFileTree shouldBe actualFileTree
      }

      test("expect directories are the same") {
        dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(dokkaHtmlDir.toFile())
        dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(dokkaHtmlDir.toFile())
      }
    }
  }


  context("Gradle caching") {

    context("expect Dokkatoo is compatible with Gradle Build Cache") {
      dokkatooProject.runner
        .addArguments(
          "clean",
          ":parentProject:dokkatooGeneratePublicationHtml",
          "--stacktrace",
        )
        .forwardOutput()
        .build {
          test("expect build is successful") {
            output shouldContain "BUILD SUCCESSFUL"
          }

          test("expect all dokka workers are successful") {
            dokkatooProject
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

      dokkatooProject.runner
        .addArguments(
          ":parentProject:dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
          "--build-cache",
        )
        .forwardOutput()
        .build {
          test("expect build is successful") {
            output shouldContain "BUILD SUCCESSFUL"
          }

          test("expect all tasks are UP-TO-DATE") {
            val nonLoggingTasks =
              tasks.filter { it.name != "logLinkDokkatooGeneratePublicationHtml" }
            nonLoggingTasks.shouldForAll {
              it shouldHaveOutcome UP_TO_DATE
            }
            tasks.shouldHaveSize(6)
          }

          test("expect Dokka Generator is not triggered") {
            // Dokka Generator shouldn't run, so check it doesn't log anything
            output shouldNotContain "Generation completed successfully"
          }
        }
    }


    context("expect Dokkatoo is compatible with Gradle Configuration Cache") {
      dokkatooProject.file(".gradle/configuration-cache").toFile().deleteRecursively()
      dokkatooProject.file("build/reports/configuration-cache").toFile().deleteRecursively()

      val configCacheRunner =
        dokkatooProject.runner
          .addArguments(
            "clean",
            ":parentProject:dokkatooGeneratePublicationHtml",
            "--stacktrace",
            "--no-build-cache",
            "--configuration-cache",
          )
          .forwardOutput()

      test("first build should store the configuration cache") {
        configCacheRunner.build {
          output shouldContain "BUILD SUCCESSFUL"
          output shouldContain "Configuration cache entry stored"
          output shouldNotContain "problems were found storing the configuration cache"
        }
      }

      test("second build should reuse the configuration cache") {
        configCacheRunner.build {
          output shouldContain "BUILD SUCCESSFUL"
          output shouldContain "Configuration cache entry reused"
        }
      }
    }
  }
})


private fun initDokkaProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("multimodule-example/dokka")

    gradleProperties = gradleProperties.lines().joinToString("\n") { line ->
      when {
        line.startsWith("dokkaVersion=") -> "dokkaVersion=$DOKKA_VERSION"
        else                             -> line
      }
    }

    settingsGradleKts = settingsGradleKts
      .replace(
        """pluginManagement {""",
        """
          |
          |pluginManagement {
          |    repositories {
          |        mavenCentral()
          |        gradlePluginPortal()
          |    }
          |
        """.trimMargin()
      ) + """
        |
        |dependencyResolutionManagement {
        |  repositories {
        |    mavenCentral()
        |  }
        |}
        |
      """.trimMargin()

    dir("parentProject") {

      buildGradleKts += """
        |
        |val hackDokkaHtmlDir by tasks.registering(Sync::class) {
        |  // sync directories so the dirs in both dokka and dokkatoo are the same
        |  from(layout.buildDirectory.dir("dokka/htmlMultiModule"))
        |  into(layout.buildDirectory.dir("dokka/html"))
        |}
        |
        |tasks.matching { it.name.contains("dokka", ignoreCase = true) && it.name != hackDokkaHtmlDir.name }.configureEach { 
        |  finalizedBy(hackDokkaHtmlDir)
        |}
        |
      """.trimMargin()
    }
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("multimodule-example/dokkatoo")
  }
}
