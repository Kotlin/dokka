package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.gradle.internal.DokkatooConstants.DOKKATOO_VERSION
import org.jetbrains.dokka.gradle.utils.*
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.TaskOutcome.SUCCESS


class KotlinDslAccessorsTest : FunSpec({

  val project = initProject()

  test("Dokkatoo DSL accessors do not trigger compilation warnings") {

    project
      .runner
      .forwardOutput()
      .addArguments(
        ":clean",
        ":compileKotlin",
        "--project-dir", "buildSrc",
        "--rerun-tasks",
        "--no-build-cache",
        "--no-configuration-cache",
      )
      .build {
        shouldHaveTaskWithOutcome(":compileKotlin", SUCCESS)
      }
  }
})


private fun initProject(
  config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
  return gradleKtsProjectTest("kotlin-dsl-accessors-test") {

    buildGradleKts = """
      |plugins {
      |  base
      |  id("dokkatoo-convention")
      |}
      |
    """.trimMargin()

    dir("buildSrc") {
      buildGradleKts = """
          |plugins {
          |  `kotlin-dsl`
          |}
          |
          |dependencies {
          |  implementation("org.jetbrains.dokka:dokka-gradle-plugin:$DOKKATOO_VERSION")
          |}
          |
          |kotlin {
          |  compilerOptions {
          |    allWarningsAsErrors.set(true)
          |  }
          |}
          |
        """.trimMargin()

      settingsGradleKts = """
          |rootProject.name = "buildSrc"
          |
          |${settingsRepositories()}
          |
        """.trimMargin()

      createKtsFile(
        "src/main/kotlin/dokkatoo-convention.gradle.kts",
        """
          |plugins {
          |  id("org.jetbrains.dokka")
          |}
          |
        """.trimMargin()
      )
    }

    config()
  }
}
