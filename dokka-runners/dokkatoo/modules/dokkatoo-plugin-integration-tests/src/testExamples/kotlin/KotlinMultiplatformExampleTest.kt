package org.jetbrains.dokka.dokkatoo.tests.examples

import org.jetbrains.dokka.dokkatoo.utils.*
import org.jetbrains.dokka.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class KotlinMultiplatformExampleTest : FunSpec({

  val dokkaProject = initDokkaProject(
    projectTestTempDir.resolve("it/examples/multiplatform-example/dokka").toFile()
  )

  val dokkatooProject = initDokkatooProject(
    projectTestTempDir.resolve("it/examples/multiplatform-example/dokkatoo").toFile()
  )

  context("compare dokka and dokkatoo HTML generators") {
    test("expect dokka can generate HTML") {
      dokkaProject.runner
        .addArguments(
          "clean",
          "dokkaHtml",
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
          ":dokkatooGeneratePublicationHtml",
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
              .shouldBeSingleton { dokkaWorkerLog ->
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
        dokkaProject.projectDir.resolve("build/dokka/html")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

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
          ":dokkatooGeneratePublicationHtml",
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
              .shouldBeSingleton { dokkaWorkerLog ->
                dokkaWorkerLog.shouldBeAFile()
                dokkaWorkerLog.readText().shouldNotContainAnyOf(
                  "[ERROR]",
                  "[WARN]",
                )
              }
          }
        }

      test("expect tasks are UP-TO-DATE") {
        dokkatooProject.runner
          .addArguments(
            ":dokkatooGeneratePublicationHtml",
            "--stacktrace",
            "--info",
            "--build-cache",
          )
          .forwardOutput()
          .build {

            output shouldContainAll listOf(
              "> Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
              "BUILD SUCCESSFUL",
              "2 actionable tasks: 2 up-to-date",
            )
            withClue("Dokka Generator should not be triggered, so check it doesn't log anything") {
              output shouldNotContain "Generation completed successfully"
            }
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
            ":dokkatooGeneratePublicationHtml",
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
    copyExampleProject("multiplatform-example/dokka")

    settingsGradleKts = settingsGradleKts
      .replace(
        """pluginManagement {""",
        """
          |
          |pluginManagement {
          |    repositories {
          |        gradlePluginPortal()
          |        mavenCentral()
          |        mavenLocal()
          |    }
          |
        """.trimMargin()
      ) + """
        |
        |dependencyResolutionManagement {
        |  repositories {
        |    mavenCentral()
        |    mavenLocal()
        |  }
        |}
        |
      """.trimMargin()

    buildGradleKts += """
        |
        |val hackDokkaHtmlDir by tasks.registering(Sync::class) {
        |  // sync directories so the dirs in both dokka and dokkatoo are the same
        |  from(layout.buildDirectory.dir("dokka/htmlMultiModule"))
        |  into(layout.buildDirectory.dir("dokka/html"))
        |}
        |
        |tasks.matching { "dokka" in it.name.toLowerCase() && it.name != hackDokkaHtmlDir.name }.configureEach { 
        |  finalizedBy(hackDokkaHtmlDir)
        |}
        |
      """.trimMargin()
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("multiplatform-example/dokkatoo")
  }
}
