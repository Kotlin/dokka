package org.jetbrains.dokka.dokkatoo

import org.jetbrains.dokka.dokkatoo.internal.DokkatooConstants
import org.jetbrains.dokka.dokkatoo.utils.*
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.sequences.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class GradlePluginProjectIntegrationTest : FunSpec({

  context("given a gradle plugin project") {
    val project = initGradlePluginProject()

    project.runner
      .addArguments(
        "clean",
        "dokkatooGeneratePublicationHtml",
        "--stacktrace",
      )
      .forwardOutput()
      .build {

        test("expect project builds successfully") {
          output shouldContain "BUILD SUCCESSFUL"
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

private fun initGradlePluginProject(
  config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
  return gradleKtsProjectTest("gradle-plugin-project") {

    settingsGradleKts += """
      |
    """.trimMargin()

    buildGradleKts = """
      |plugins {
      |  `kotlin-dsl`
      |  id("org.jetbrains.dokka.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
      |}
      |
    """.trimMargin()

    dir("src/main/kotlin") {

      createKotlinFile(
        "MyCustomGradlePlugin.kt",
        """
          |package com.project.gradle.plugin
          |
          |import javax.inject.Inject
          |import org.gradle.api.Plugin
          |import org.gradle.api.Project
          |import org.gradle.api.model.ObjectFactory
          |import org.gradle.kotlin.dsl.*
          |
          |abstract class MyCustomGradlePlugin @Inject constructor(
          |  private val objects: ObjectFactory
          |) : Plugin<Project> {
          |  override fun apply(project: Project) {
          |    println(objects.property<String>().getOrElse("empty"))
          |  }
          |}

        """.trimMargin()
      )

      createKotlinFile(
        "MyCustomGradlePluginExtension.kt",
        """
          |package com.project.gradle.plugin
          |
          |import org.gradle.api.provider.*
          |
          |interface MyCustomGradlePluginExtension {
          |  val versionProperty: Property<String>
          |  val versionProvider: Provider<String>
          |}
          |
        """.trimMargin()
      )
    }

    config()
  }
}
