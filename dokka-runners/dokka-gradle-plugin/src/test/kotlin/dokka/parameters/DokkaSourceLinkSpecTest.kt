package org.jetbrains.dokka.gradle.dokka.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.extraProperties

class DokkaSourceLinkSpecTest : FunSpec({
  val project = ProjectBuilder.builder().build()
  project.extraProperties.set("enableDokkatoo", true)

  context("expect localDirectoryPath") {
    test("is the invariantSeparatorsPath of localDirectory") {
      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(project.rootDir.resolve("some/nested/dir"))
      }

      actual.localDirectoryPath.get() shouldBe "some/nested/dir"
    }
  }


  context("expect remoteUrl can be set") {
    test("using a string") {
      val actual = project.createDokkaSourceLinkSpec {
        remoteUrl("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.remoteUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val actual = project.createDokkaSourceLinkSpec {
        remoteUrl(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.remoteUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }
}) {

  companion object {
    private fun Project.createDokkaSourceLinkSpec(
      configure: DokkaSourceLinkSpec.() -> Unit
    ): DokkaSourceLinkSpec =
      objects.newInstance(DokkaSourceLinkSpec::class).apply(configure)
  }
}
