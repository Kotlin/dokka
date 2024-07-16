package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.utils.*
import dev.adamko.dokkatoo.utils.projects.initMultiModuleProject
import io.kotest.core.spec.style.FunSpec

class DuplicateModulePathWarningTest : FunSpec({

  context("when subprojects have duplicate modulePaths") {
    val project = initMultiModuleProject("DuplicateModulePath")

    project.dir("subproject-hello") {
      buildGradleKts += """
        |
        |dokkatoo {
        |  modulePath = "dupe"
        |}
        |
      """.trimMargin()
    }

    project.dir("subproject-goodbye") {
      buildGradleKts += """
        |
        |dokkatoo {
        |  modulePath = "dupe"
        |}
        |
      """.trimMargin()
    }

    context("generate HTML publication") {
      project.runner
        .addArguments(
          ":dokkatooGeneratePublicationHtml",
          "--rerun-tasks",
          "--stacktrace",
          "--warn",
        )
        .forwardOutput()
        .build {
          test("expect duplicate module path warning") {
            output.shouldContainAll(
              "[:dokkatooGeneratePublicationHtml] Duplicate `modulePath`s in Dokka Generator parameters.",
              "- 'subproject-hello', 'subproject-goodbye' have modulePath 'dupe'",
            )
          }
        }
    }
  }
})
