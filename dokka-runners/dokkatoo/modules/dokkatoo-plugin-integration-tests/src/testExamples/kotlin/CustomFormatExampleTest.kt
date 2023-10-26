package org.jetbrains.dokka.dokkatoo.tests.examples

import org.jetbrains.dokka.dokkatoo.internal.DokkatooConstants.DOKKA_VERSION
import org.jetbrains.dokka.dokkatoo.utils.*
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class CustomFormatExampleTest : FunSpec({

  val dokkaProject = initDokkaProject(
    GradleProjectTest.projectTestTempDir.resolve("it/examples/custom-format-dokka").toFile()
  )

  val dokkatooProject = initDokkatooProject(
    GradleProjectTest.projectTestTempDir.resolve("it/examples/custom-format-dokkatoo").toFile()
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

    test("expect dokkatoo can generate HTML") {
      dokkatooProject.runner
        .addArguments(
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build {
          output shouldContain "BUILD SUCCESSFUL"

          dokkatooProject
            .findFiles { it.name == "dokka-worker.log" }
            .shouldBeSingleton { dokkaWorkerLog ->
              dokkaWorkerLog.shouldNotBeNull().shouldBeAFile()
              dokkaWorkerLog.readText() shouldContain "Generation completed successfully"
            }
        }
    }

    context("expect dokka and dokkatoo HTML is the same") {
      val dokkaHtmlDir = dokkaProject.projectDir.resolve("build/dokka/html")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

      test("expect file trees are the same") {
        val expectedFileTree = dokkaHtmlDir.toTreeString()
        val actualFileTree = dokkatooHtmlDir.toTreeString()
        println((actualFileTree to expectedFileTree).sideBySide())
        // drop the first line from each, since the directory name is different
        expectedFileTree.substringAfter("\n") shouldBe actualFileTree.substringAfter("\n")
      }

      test("expect directories are the same") {
        dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(dokkaHtmlDir.toFile())
        dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(dokkaHtmlDir.toFile())
      }
    }
  }


  context("Gradle caching") {
    test("expect Dokkatoo is compatible with Gradle Build Cache") {
      dokkatooProject.runner
        .addArguments(
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build {
          output shouldContain "BUILD SUCCESSFUL"

          dokkatooProject
            .findFiles { it.name == "dokka-worker.log" }
            .shouldBeSingleton { dokkaWorkerLog ->
              dokkaWorkerLog.shouldNotBeNull().shouldBeAFile()
              dokkaWorkerLog.readText() shouldContain "Generation completed successfully"
            }
        }

      dokkatooProject.runner
        .addArguments(
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--build-cache",
          "--info",
        )
        .forwardOutput()
        .build {
          output shouldContainAll listOf(
            "> Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
            "BUILD SUCCESSFUL",
            "1 actionable task: 1 up-to-date",
          )
          withClue("Dokka Generator should not be triggered, so check it doesn't log anything") {
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
    copyExampleProject("custom-format-example/dokka")

    buildGradleKts = buildGradleKts
      .replace(
        Regex("""id\("org\.jetbrains\.dokka"\) version \("[\d.]+"\)"""),
        Regex.escapeReplacement("""id("org.jetbrains.dokka") version "$DOKKA_VERSION""""),
      )
      .replace(
        "org.jetbrains.dokka:dokka-base:1.7.10",
        "org.jetbrains.dokka:dokka-base:1.7.20",
      )

    settingsGradleKts = settingsGradleKts
      .replace(
        """rootProject.name = "dokka-customFormat-example"""",
        """rootProject.name = "customFormat-example"""",
      )
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("custom-format-example/dokkatoo")

    buildGradleKts += """
      |
      |tasks.withType<org.jetbrains.dokka.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
      |  generator.dokkaSourceSets.configureEach {
      |    sourceSetScope.set(":dokkaHtml") // only necessary for testing
      |  }
      |}
      |
    """.trimMargin()
  }
}
