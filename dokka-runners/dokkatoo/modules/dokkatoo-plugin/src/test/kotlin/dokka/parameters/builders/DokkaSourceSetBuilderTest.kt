package org.jetbrains.dokka.dokkatoo.dokka.parameters.builders

import org.jetbrains.dokka.dokkatoo.DokkatooExtension
import org.jetbrains.dokka.dokkatoo.DokkatooPlugin
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.dokkatoo.utils.all_
import org.jetbrains.dokka.dokkatoo.utils.create_
import org.jetbrains.dokka.dokkatoo.utils.shouldContainAll
import org.jetbrains.dokka.dokkatoo.utils.sourceLink_
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.net.URI
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder

class DokkaSourceSetBuilderTest : FunSpec({

  context("when building a ExternalDocumentationLinkSpec") {
    val project = createProject()

    test("expect url is required") {
      val sourceSetSpec = project.createDokkaSourceSetSpec("test1") {
        externalDocumentationLinks.create_("TestLink") {
          url.set(null as URI?)
          packageListUrl("https://github.com/adamko-dev/dokkatoo/")
        }
      }

      val caughtException = shouldThrow<MissingValueException> {
        DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec))
      }

      caughtException.message shouldContain "Cannot query the value of property 'url' because it has no value available"
    }

    test("expect packageListUrl is required") {
      val sourceSetSpec = project.createDokkaSourceSetSpec("test2") {
        externalDocumentationLinks.create_("TestLink") {
          url("https://github.com/adamko-dev/dokkatoo/")
          packageListUrl.convention(null as URI?)
          packageListUrl.set(null as URI?)
        }
      }

      val caughtException = shouldThrow<MissingValueException> {
        DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec))
      }

      caughtException.message shouldContain "Cannot query the value of property 'packageListUrl' because it has no value available"
    }

    test("expect null when not enabled") {
      val sourceSetSpec = project.createDokkaSourceSetSpec("test3")
      val linkSpec = sourceSetSpec.externalDocumentationLinks.create_("TestLink") {
        url("https://github.com/adamko-dev/dokkatoo/")
        packageListUrl("https://github.com/adamko-dev/dokkatoo/")
        enabled.set(false)
      }

      DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec)).shouldBeSingleton { sourceSet ->
        sourceSet.externalDocumentationLinks.shouldForAll { link ->
          link.url shouldNotBeEqual linkSpec.url.get().toURL()
          link.packageListUrl shouldNotBeEqual linkSpec.packageListUrl.get().toURL()
        }
      }
    }
  }


  context("when DokkaSourceLinkSpec is built") {
    val project = createProject()

    test("expect built object contains all properties") {
      val tempDir = tempdir()

      val sourceSetSpec = project.createDokkaSourceSetSpec("testAllProperties") {
        sourceLink_ {
          localDirectory.set(tempDir)
          remoteUrl("https://github.com/adamko-dev/dokkatoo/")
          remoteLineSuffix.set("%L")
        }
      }

      val sourceSet = DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec)).single()

      sourceSet.sourceLinks.shouldBeSingleton { sourceLink ->
        sourceLink.remoteUrl shouldBe URI("https://github.com/adamko-dev/dokkatoo/").toURL()
        sourceLink.localDirectory shouldBe tempDir.invariantSeparatorsPath
        sourceLink.remoteLineSuffix shouldBe "%L"
      }
    }

    test("expect localDirectory is required") {
      val sourceSetSpec = project.createDokkaSourceSetSpec("testLocalDirRequired") {
        sourceLink_ {
          remoteUrl("https://github.com/adamko-dev/dokkatoo/")
          remoteLineSuffix.set("%L")
        }
      }

      sourceSetSpec.sourceLinks.all_ {
        localDirectory.convention(null as Directory?)
        localDirectory.set(null as File?)
      }

      val caughtException = shouldThrow<MissingValueException> {
        DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec))
      }

      caughtException.message.shouldContainAll(
        "Cannot query the value of this provider because it has no value available",
        "The value of this provider is derived from",
        "property 'localDirectory'",
      )
    }

    test("expect localDirectory is an invariantSeparatorsPath") {
      val tempDir = tempdir()

      val sourceSetSpec = project.createDokkaSourceSetSpec("testLocalDirPath") {
        sourceLink_ {
          localDirectory.set(tempDir)
          remoteUrl("https://github.com/adamko-dev/dokkatoo/")
          remoteLineSuffix.set(null as String?)
        }
      }

      val link = DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec))
        .single()
        .sourceLinks
        .single()

      link.localDirectory shouldBe tempDir.invariantSeparatorsPath
    }

    test("expect remoteUrl is required") {
      val sourceSetSpec = project.createDokkaSourceSetSpec("testRemoteUrlRequired") {
        sourceLink_ {
          localDirectory.set(tempdir())
          remoteUrl.set(project.providers.provider { null })
          remoteLineSuffix.set("%L")
        }
      }

      val caughtException = shouldThrow<MissingValueException> {
        DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec))
      }

      caughtException.message shouldContain "Cannot query the value of property 'remoteUrl' because it has no value available"
    }

    test("expect remoteLineSuffix is optional") {
      val tempDir = tempdir()

      val sourceSetSpec = project.createDokkaSourceSetSpec("testRemoteLineSuffixOptional") {
        sourceLink_ {
          localDirectory.set(tempDir)
          remoteUrl("https://github.com/adamko-dev/dokkatoo/")
          remoteLineSuffix.set(project.providers.provider { null })
        }
      }

      val sourceSet = DokkaSourceSetBuilder.buildAll(setOf(sourceSetSpec)).single()

      sourceSet.sourceLinks.shouldBeSingleton { sourceLink ->
        sourceLink.remoteUrl shouldBe URI("https://github.com/adamko-dev/dokkatoo/").toURL()
        sourceLink.localDirectory shouldBe tempDir.invariantSeparatorsPath
        sourceLink.remoteLineSuffix shouldBe null
      }
    }
  }
})

private fun createProject(): Project {
  val project = ProjectBuilder.builder().build()
  project.plugins.apply(type = DokkatooPlugin::class)
  return project
}

private fun Project.createDokkaSourceSetSpec(
  name: String,
  configure: DokkaSourceSetSpec.() -> Unit = {}
): DokkaSourceSetSpec {
  return extensions
    .getByType<DokkatooExtension>()
    .dokkatooSourceSets
    .create_(name, configure)
}
