package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.utils.addArguments
import dev.adamko.dokkatoo.utils.build
import dev.adamko.dokkatoo.utils.name
import dev.adamko.dokkatoo.utils.projects.initMultiModuleProject
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.shouldForAll
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContainIgnoringCase

class DokkatooBaseTasksLinksTest : FunSpec({

  context("Verify that the base lifecycle tasks do not trigger Dokkatoo tasks") {
    val project = initMultiModuleProject("TaskLinks")

    withData(
      "assemble",
      "build",
      "check",
      "clean",
    ) { baseTask ->
      project.runner
        .addArguments(
          baseTask,
          "--quiet",
        )
        .build {
          tasks.shouldForOne { it.path shouldBe ":${baseTask}" }
          tasks.shouldForOne { it.path shouldBe ":subproject-goodbye:${baseTask}" }
          tasks.shouldForOne { it.path shouldBe ":subproject-hello:${baseTask}" }

          tasks.shouldForAll { task ->
            task.name shouldNotContainIgnoringCase "dokkatoo"
          }
        }
    }
  }
})
