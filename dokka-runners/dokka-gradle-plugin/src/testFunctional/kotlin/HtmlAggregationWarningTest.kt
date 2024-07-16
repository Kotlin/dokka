package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.utils.addArguments
import dev.adamko.dokkatoo.utils.build
import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.projects.initMultiModuleProject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class HtmlAggregationWarningTest : FunSpec({
  context("when all-modules-page-plugin is missing") {
    val project = initMultiModuleProject("no-all-pages-plugin")

    project.buildGradleKts += """
      |
      |// hack, to remove all-modules-page-plugin for testing purposes
      |configurations.all { 
      |  exclude("org.jetbrains.dokka", "all-modules-page-plugin")
      |}
    """.trimMargin()


    project.runner
      .addArguments(
        "clean",
        ":dokkatooGenerate",
        "--stacktrace",
        "--info",
      )
      .forwardOutput()
      .build {
        test("expect warning message is logged") {
          output shouldContain expectedWarning
          output shouldContain allPlugins
        }
      }
  }

  context("when all-modules-page-plugin is present") {
    val project = initMultiModuleProject("with-all-pages-plugin")

    project.runner
      .addArguments(
        "clean",
        ":dokkatooGenerate",
        "--stacktrace",
        "--info",
      )
      .forwardOutput()
      .build {
        test("expect warning message is not logged") {
          output shouldNotContain expectedWarning
          output shouldNotContain allPlugins
        }
      }
  }
}) {
  companion object {
    private val expectedWarning = /* language=text */ """
        |[:dokkatooGeneratePublicationHtml] org.jetbrains.dokka:all-modules-page-plugin is missing.
        |
        |Publication 'test' in has 2 modules, but
        |the Dokka Generator plugins classpath does not contain 
        |   org.jetbrains.dokka:all-modules-page-plugin
        |which is required for aggregating Dokka HTML modules.
        |
        |Dokkatoo should have added org.jetbrains.dokka:all-modules-page-plugin automatically.
        |
        |Generation will proceed, but the generated output might not contain the full HTML docs.
        |
        |Suggestions:
        | - Verify that the dependency has not been excluded.
        | - Raise an issue https://github.com/adamko-dev/dokkatoo/issues
      """
      .trimMargin()
      .prependIndent("> ")

    private val allPlugins = /* language=text */ """
        |(all plugins: org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.templates.TemplatingPlugin)
      """
      .trimMargin()
      .prependIndent("> ")
  }
}
