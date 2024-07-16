package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.internal.DokkatooConstants.DOKKATOO_VERSION
import dev.adamko.dokkatoo.utils.*
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
          |  implementation("dev.adamko.dokkatoo:dokka-gradle-plugin:$DOKKATOO_VERSION")
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
          |  id("dev.adamko.dokkatoo")
          |}
          |
        """.trimMargin()
      )
    }

    config()
  }
}
