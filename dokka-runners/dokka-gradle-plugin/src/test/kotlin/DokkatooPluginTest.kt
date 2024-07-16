package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.utils.create_
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder

class DokkatooPluginTest : FunSpec({

  test("expect plugin id can be applied to project successfully") {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("dev.adamko.dokkatoo")
    project.plugins.hasPlugin("dev.adamko.dokkatoo") shouldBe true
    project.plugins.hasPlugin(DokkatooPlugin::class) shouldBe true
  }

  test("expect plugin class can be applied to project successfully") {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply(type = DokkatooPlugin::class)
    project.plugins.hasPlugin("dev.adamko.dokkatoo") shouldBe true
    project.plugins.hasPlugin(DokkatooPlugin::class) shouldBe true
  }

  context("Dokkatoo property conventions") {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("dev.adamko.dokkatoo")

    val extension = project.extensions.getByType<DokkatooExtension>()

    context("DokkatooSourceSets") {
      val testSourceSet = extension.dokkatooSourceSets.create_("Test") {
        externalDocumentationLinks.create_("gradle") {
          url("https://docs.gradle.org/7.6.1/javadoc")
        }
      }

      context("JDK external documentation link") {
        val jdkLink = testSourceSet.externalDocumentationLinks.getByName("jdk")

        test("when enableJdkDocumentationLink is false, expect jdk link is disabled") {
          testSourceSet.enableJdkDocumentationLink.set(false)
          jdkLink.enabled.get() shouldBe false
        }

        test("when enableJdkDocumentationLink is true, expect jdk link is enabled") {
          testSourceSet.enableJdkDocumentationLink.set(true)
          jdkLink.enabled.get() shouldBe true
        }

        (5..10).forEach { jdkVersion ->
          test("when jdkVersion is $jdkVersion, expect packageListUrl uses package-list file") {
            testSourceSet.jdkVersion.set(jdkVersion)
            jdkLink.packageListUrl.get().toString() shouldEndWith "package-list"
          }
        }

        (11..22).forEach { jdkVersion ->
          test("when jdkVersion is $jdkVersion, expect packageListUrl uses element-list file") {
            testSourceSet.jdkVersion.set(jdkVersion)
            jdkLink.packageListUrl.get().toString() shouldEndWith "element-list"
          }
        }
      }

      context("external doc links") {
        test("package-list url should be appended to Javadoc URL") {
          val gradleDocLink = testSourceSet.externalDocumentationLinks.getByName("gradle")
          gradleDocLink.packageListUrl.get()
            .toString() shouldBe "https://docs.gradle.org/7.6.1/javadoc/package-list"
        }
      }
    }
  }
})
