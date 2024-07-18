package org.jetbrains.dokka.gradle.dokka.parameters

import org.jetbrains.dokka.gradle.DokkatooExtension
import org.jetbrains.dokka.gradle.DokkatooPlugin
import org.jetbrains.dokka.gradle.utils.create_
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.extraProperties


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
  project.extraProperties.set("enableDokkatoo", true)
  project.plugins.apply(type = DokkatooPlugin::class)
}

private fun createExternalDocLinkSpec(
  configure: DokkaExternalDocumentationLinkSpec.() -> Unit
): DokkaExternalDocumentationLinkSpec {
  val dssContainer = project.extensions.getByType<DokkatooExtension>().dokkatooSourceSets

  return dssContainer.create("test" + dssContainer.size)
    .externalDocumentationLinks
    .create("testLink", configure)
}
