package dev.adamko.dokkatoo.dokka.plugins

import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.DokkatooPlugin
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder

class DokkaVersioningPluginParametersTest : FunSpec({
  val project = ProjectBuilder.builder().build().also { project ->
    project.plugins.apply(type = DokkatooPlugin::class)
  }

  fun TestScope.versioningPluginParams(
    configure: DokkaVersioningPluginParameters.() -> Unit = {}
  ): DokkaVersioningPluginParameters =
    project.extensions
      .getByType<DokkatooExtension>()
      .pluginsConfiguration
      .create<DokkaVersioningPluginParameters>(testCase.name.testName, configure)


  context("when params have default convention values") {
    val params = versioningPluginParams {
      // no configuration, default values
    }

    test("expect version is null") {
      params.version.orNull.shouldBeNull()
    }
    test("expect versionsOrdering is empty list") {
      params.versionsOrdering.orNull.shouldBeEmpty()
    }
    test("expect olderVersionsDir is null") {
      params.olderVersionsDir.orNull.shouldBeNull()
    }
    test("expect olderVersions is empty") {
      params.olderVersions.shouldBeEmpty()
    }
    test("expect renderVersionsNavigationOnAllPages is true") {
      params.renderVersionsNavigationOnAllPages.orNull shouldBe true
    }

    test("expect correct JSON") {
      params.jsonEncode() shouldEqualJson /* language=JSON */ """
        |{
        |  "olderVersions": [],
        |  "renderVersionsNavigationOnAllPages": true
        |}
      """.trimMargin()
    }
  }

  context("when params are set, expect correct JSON") {
    val params = versioningPluginParams {
      // no configuration, default values
      version.set("x.y.z")
      versionsOrdering.set(listOf("a.b.c", "x.y.z", "1.2.3"))
      olderVersionsDir.set(project.layout.buildDirectory.dir("older-versions-dir"))
      olderVersions.from(project.layout.buildDirectory.dir("older-versions"))
      renderVersionsNavigationOnAllPages.set(false)
    }

    test("expect correct JSON") {
      val buildDir = project.layout.buildDirectory.get().asFile.invariantSeparatorsPath
      params.jsonEncode() shouldEqualJson /* language=JSON */ """
        |{
        |  "version": "x.y.z",
        |  "versionsOrdering": [
        |    "a.b.c",
        |    "x.y.z",
        |    "1.2.3"
        |  ],
        |  "olderVersionsDir": "${buildDir}/older-versions-dir",
        |  "olderVersions": [
        |    "${buildDir}/older-versions"
        |  ],
        |  "renderVersionsNavigationOnAllPages": false
        |}
      """.trimMargin()
    }
  }


  context("when versionsOrdering are set as an empty list") {
    val params = versioningPluginParams {
      versionsOrdering.set(emptyList())
    }

    test("expect versionsOrdering is null") {
      params.versionsOrdering.orNull.shouldBeEmpty()
    }

    test("expect versionsOrdering not present in JSON") {
      params.jsonEncode() shouldEqualJson /* language=JSON */ """
        |{
        |  "olderVersions": [],
        |  "renderVersionsNavigationOnAllPages": true
        |}
      """.trimMargin()
    }
  }
})
