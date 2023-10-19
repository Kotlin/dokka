package org.jetbrains.dokka.dokkatoo

import org.jetbrains.dokka.dokkatoo.internal.DokkatooConstants
import org.jetbrains.dokka.dokkatoo.utils.*
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.sequences.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class KotlinMultiplatformFunctionalTest : FunSpec({

  context("when dokkatoo generates all formats") {
    val project = initKotlinMultiplatformProject()

    project.runner
      .addArguments(
        "clean",
        ":dokkatooGeneratePublicationHtml",
        "--stacktrace",
      )
      .forwardOutput()
      .build {
        test("expect build is successful") {
          output shouldContain "BUILD SUCCESSFUL"
        }
      }

    test("expect all dokka workers are successful") {
      project
        .findFiles { it.name == "dokka-worker.log" }
        .shouldBeSingleton { dokkaWorkerLog ->
          dokkaWorkerLog.shouldBeAFile()
          dokkaWorkerLog.readText().shouldNotContainAnyOf(
            "[ERROR]",
            "[WARN]",
          )
        }
    }

    context("expect HTML site is generated") {

      test("with expected HTML files") {
        project.projectDir.resolve("build/dokka/html/index.html").shouldBeAFile()
        project.projectDir.resolve("build/dokka/html/com/project/hello/Hello.html")
          .shouldBeAFile()
      }

      test("and dokka_parameters.json is generated") {
        project.projectDir.resolve("build/dokka/html/dokka_parameters.json")
          .shouldBeAFile()
      }

      test("with element-list") {
        project.projectDir.resolve("build/dokka/html/test/package-list").shouldBeAFile()
        project.projectDir.resolve("build/dokka/html/test/package-list").toFile().readText()
          .sortLines()
          .shouldContain( /* language=text */ """
              |${'$'}dokka.format:html-v1
              |${'$'}dokka.linkExtension:html
              |${'$'}dokka.location:com.project////PointingToDeclaration/test/com.project/index.html
              |${'$'}dokka.location:com.project//goodbye/#kotlinx.serialization.json.JsonObject/PointingToDeclaration/test/com.project/goodbye.html
              |${'$'}dokka.location:com.project/Hello///PointingToDeclaration/test/com.project/-hello/index.html
              |${'$'}dokka.location:com.project/Hello/Hello/#/PointingToDeclaration/test/com.project/-hello/-hello.html
              |${'$'}dokka.location:com.project/Hello/sayHello/#kotlinx.serialization.json.JsonObject/PointingToDeclaration/test/com.project/-hello/say-hello.html
              |com.project
            """.trimMargin()
          )
      }

      test("expect no 'unknown class' message in HTML files") {
        val htmlFiles = project.projectDir.toFile()
          .resolve("build/dokka/html")
          .walk()
          .filter { it.isFile && it.extension == "html" }

        htmlFiles.shouldNotBeEmpty()

        htmlFiles.forEach { htmlFile ->
          val relativePath = htmlFile.relativeTo(project.projectDir.toFile())
          withClue("$relativePath should not contain Error class: unknown class") {
            htmlFile.useLines { lines ->
              lines.shouldForAll { line -> line.shouldNotContain("Error class: unknown class") }
            }
          }
        }
      }
    }
  }
})


private fun initKotlinMultiplatformProject(
  config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
  return gradleKtsProjectTest("kotlin-multiplatform-project") {

    settingsGradleKts += """
      |
      |dependencyResolutionManagement {
      |
      |  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
      |
      |  repositories {
      |    mavenCentral()
      |
      |    // Declare the Node.js & Yarn download repositories
      |    exclusiveContent {
      |      forRepository {
      |        ivy("https://nodejs.org/dist/") {
      |          name = "Node Distributions at ${'$'}url"
      |          patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
      |          metadataSources { artifact() }
      |          content { includeModule("org.nodejs", "node") }
      |        }
      |      }
      |      filter { includeGroup("org.nodejs") }
      |    }
      |
      |    exclusiveContent {
      |      forRepository {
      |        ivy("https://github.com/yarnpkg/yarn/releases/download") {
      |          name = "Node Distributions at ${'$'}url"
      |          patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
      |          metadataSources { artifact() }
      |          content { includeModule("com.yarnpkg", "yarn") }
      |        }
      |      }
      |      filter { includeGroup("com.yarnpkg") }
      |    }
      |  }
      |}
      |
    """.trimMargin()

    buildGradleKts = """
      |plugins {
      |  kotlin("multiplatform") version "1.8.22"
      |  id("org.jetbrains.dokka.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
      |}
      |
      |kotlin {
      |  jvm()
      |  js(IR) {
      |    browser()
      |  }
      |
      |  sourceSets {
      |    commonMain {
      |      dependencies {
      |        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
      |      }
      |    }
      |    commonTest {
      |      dependencies {
      |        implementation(kotlin("test"))
      |      }
      |    }
      |  }
      |}
      |
      |dependencies {
      |  // must manually add this dependency for aggregation to work
      |  //dokkatooPluginHtml("org.jetbrains.dokka:all-modules-page-plugin:1.8.10")
      |}
      |
      |dokkatoo {
      |  dokkatooSourceSets.configureEach {
      |    externalDocumentationLinks {
      |      create("kotlinxSerialization") {
      |        url("https://kotlinlang.org/api/kotlinx.serialization/")
      |      }
      |    }
      |  }
      |}
      |
      |
    """.trimMargin()

    dir("src/commonMain/kotlin/") {

      createKotlinFile(
        "Hello.kt",
        """
          |package com.project
          |
          |import kotlinx.serialization.json.JsonObject
          |
          |/** The Hello class */
          |class Hello {
          |    /** prints `Hello` and [json] to the console */
          |    fun sayHello(json: JsonObject) = println("Hello ${'$'}json")
          |}
          |
        """.trimMargin()
      )

      createKotlinFile(
        "goodbye.kt",
        """
          |package com.project
          |
          |import kotlinx.serialization.json.JsonObject
          |
          |/** Should print `goodbye` and [json] to the console */
          |expect fun goodbye(json: JsonObject)
          |
        """.trimMargin()
      )
    }

    dir("src/jvmMain/kotlin/") {
      createKotlinFile(
        "goodbyeJvm.kt",
        """
          |package com.project
          |
          |import kotlinx.serialization.json.JsonObject
          |
          |/** JVM implementation - prints `goodbye` and [json] to the console */
          |actual fun goodbye(json: JsonObject) = println("[JVM] goodbye ${'$'}json")
          |
        """.trimMargin()
      )
    }

    dir("src/jsMain/kotlin/") {
      createKotlinFile(
        "goodbyeJs.kt",
        """
          |package com.project
          |
          |import kotlinx.serialization.json.JsonObject
          |
          |/** JS implementation - prints `goodbye` and [json] to the console */
          |actual fun goodbye(json: JsonObject) = println("[JS] goodbye ${'$'}json")
          |
        """.trimMargin()
      )
    }

    config()
  }
}
