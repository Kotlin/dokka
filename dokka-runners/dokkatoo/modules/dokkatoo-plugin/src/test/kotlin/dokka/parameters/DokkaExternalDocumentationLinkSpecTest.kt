package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.dokkatoo.DokkatooExtension
import org.jetbrains.dokka.dokkatoo.DokkatooPlugin
import org.jetbrains.dokka.dokkatoo.utils.create_
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder


class DokkaExternalDocumentationLinkSpecTest : FunSpec({

  context("expect url can be set") {
    test("using a string") {
      val actual = createExternalDocLinkSpec {
        url("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.url.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val actual = createExternalDocLinkSpec {
        url(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.url.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }

  context("expect packageListUrl can be set") {
    test("using a string") {
      val actual = createExternalDocLinkSpec {
        packageListUrl("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.packageListUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val actual = createExternalDocLinkSpec {
        packageListUrl(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.packageListUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }

  context("expect packageList defaults to url+package-list") {
    data class TestCase(
      val actualUrl: String,
      val expected: String,
      val testName: String,
    ) : WithDataTestName {
      override fun dataTestName(): String = testName
    }

    withData(
      TestCase(
        testName = "non-empty path, with trailing slash",
        actualUrl = "https://github.com/adamko-dev/dokkatoo/",
        expected = "https://github.com/adamko-dev/dokkatoo/package-list",
      ),
      TestCase(
        testName = "non-empty path, without trailing slash",
        actualUrl = "https://github.com/adamko-dev/dokkatoo",
        expected = "https://github.com/adamko-dev/dokkatoo/package-list",
      ),
      TestCase(
        testName = "empty path, with trailing slash",
        actualUrl = "https://github.com/",
        expected = "https://github.com/package-list",
      ),
      TestCase(
        testName = "empty path, without trailing slash",
        actualUrl = "https://github.com",
        expected = "https://github.com/package-list",
      )
    ) { (actualUrl, expected) ->
      val actual = createExternalDocLinkSpec { url(actualUrl) }
      actual.packageListUrl.get().toString() shouldBe expected
    }
  }
})

private val project = ProjectBuilder.builder().build().also { project ->
  project.plugins.apply(type = DokkatooPlugin::class)
}

private fun createExternalDocLinkSpec(
  configure: DokkaExternalDocumentationLinkSpec.() -> Unit
): DokkaExternalDocumentationLinkSpec {

  val dssContainer = project.extensions.getByType<DokkatooExtension>().dokkatooSourceSets

  return dssContainer.create_("test" + dssContainer.size)
    .externalDocumentationLinks
    .create("testLink", configure)
}
