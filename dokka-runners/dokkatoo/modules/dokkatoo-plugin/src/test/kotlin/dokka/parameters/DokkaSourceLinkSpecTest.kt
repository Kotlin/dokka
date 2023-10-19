package org.jetbrains.dokka.dokkatoo.dokka.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder

class DokkaSourceLinkSpecTest : FunSpec({
  val project = ProjectBuilder.builder().build()

  context("expect localDirectoryPath") {
    test("is the invariantSeparatorsPath of localDirectory") {
      val tempDir = tempdir()

      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(tempDir)
      }

      actual.localDirectoryPath2.get() shouldBe tempDir.invariantSeparatorsPath
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

  /** Re-implement [DokkaSourceLinkSpec] to make [localDirectoryPath] accessible in tests */
  abstract class DokkaSourceLinkSpec2 : DokkaSourceLinkSpec() {
    val localDirectoryPath2: Provider<String>
      get() = super.localDirectoryPath
  }

  companion object {
    private fun Project.createDokkaSourceLinkSpec(
      configure: DokkaSourceLinkSpec.() -> Unit
    ): DokkaSourceLinkSpec2 =
      objects.newInstance(DokkaSourceLinkSpec2::class).apply(configure)
  }
}
