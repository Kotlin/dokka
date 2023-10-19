package org.jetbrains.dokka.dokkatoo

import org.jetbrains.dokka.dokkatoo.internal.DokkatooConstants.DOKKATOO_VERSION
import org.jetbrains.dokka.dokkatoo.utils.*
import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.string.shouldContain

class DokkatooPluginFunctionalTest : FunSpec({
  val testProject = gradleKtsProjectTest("DokkatooPluginFunctionalTest") {
    buildGradleKts = """
      |plugins {
      |  id("org.jetbrains.dokka.dokkatoo") version "$DOKKATOO_VERSION"
      |}
      |
    """.trimMargin()
  }

  test("expect Dokka Plugin creates Dokka tasks") {
    testProject.runner
      .addArguments("tasks", "--group=dokkatoo", "-q")
      .build {
        withClue(output) {
          val dokkatooTasks = output
            .substringAfter("Dokkatoo tasks")
            .lines()
            .filter { it.contains(" - ") }
            .associate { it.splitToPair(" - ") }

          dokkatooTasks.shouldContainExactly(
            //@formatter:off
            "dokkatooGenerate"                       to "Generates Dokkatoo publications for all formats",
            "dokkatooGenerateModuleGfm"              to "Executes the Dokka Generator, generating a gfm module",
            "dokkatooGenerateModuleHtml"             to "Executes the Dokka Generator, generating a html module",
            "dokkatooGenerateModuleJavadoc"          to "Executes the Dokka Generator, generating a javadoc module",
            "dokkatooGenerateModuleJekyll"           to "Executes the Dokka Generator, generating a jekyll module",
            "dokkatooGeneratePublicationGfm"         to "Executes the Dokka Generator, generating the gfm publication",
            "dokkatooGeneratePublicationHtml"        to "Executes the Dokka Generator, generating the html publication",
            "dokkatooGeneratePublicationJavadoc"     to "Executes the Dokka Generator, generating the javadoc publication",
            "dokkatooGeneratePublicationJekyll"      to "Executes the Dokka Generator, generating the jekyll publication",
            "prepareDokkatooModuleDescriptorGfm"     to "Prepares the Dokka Module Descriptor for gfm",
            "prepareDokkatooModuleDescriptorHtml"    to "Prepares the Dokka Module Descriptor for html",
            "prepareDokkatooModuleDescriptorJavadoc" to "Prepares the Dokka Module Descriptor for javadoc",
            "prepareDokkatooModuleDescriptorJekyll"  to "Prepares the Dokka Module Descriptor for jekyll",
            //@formatter:on
          )
        }
      }
  }

  test("expect Dokka Plugin creates Dokka outgoing variants") {
    val build = testProject.runner
      .addArguments("outgoingVariants", "-q")
      .build {
        val variants = output.invariantNewlines().replace('\\', '/')

        val dokkatooVariants = variants.lines()
          .filter { it.contains("dokka", ignoreCase = true) }
          .mapNotNull { it.substringAfter("Variant ", "").takeIf(String::isNotBlank) }


        dokkatooVariants.shouldContainExactlyInAnyOrder(
          "dokkatooModuleElementsGfm",
          "dokkatooModuleElementsHtml",
          "dokkatooModuleElementsJavadoc",
          "dokkatooModuleElementsJekyll",
        )

        fun checkVariant(format: String) {
          val formatCapitalized = format.uppercaseFirstChar()

          variants shouldContain /* language=text */ """
            |--------------------------------------------------
            |Variant dokkatooModuleElements$formatCapitalized
            |--------------------------------------------------
            |Provide Dokka Module files for $format to other subprojects
            |
            |Capabilities
            |    - :test:unspecified (default capability)
            |Attributes
            |    - org.jetbrains.dokka.dokkatoo.base     = dokkatoo
            |    - org.jetbrains.dokka.dokkatoo.category = module-files
            |    - org.jetbrains.dokka.dokkatoo.format   = $format
            |Artifacts
            |    - build/dokka-config/$format/module_descriptor.json (artifactType = json)
            |    - build/dokka-module/$format (artifactType = directory)
            |
          """.trimMargin()
        }

        checkVariant("gfm")
        checkVariant("html")
        checkVariant("javadoc")
        checkVariant("jekyll")
      }
  }

  test("expect Dokka Plugin creates Dokka resolvable configurations") {

    val expectedFormats = listOf("Gfm", "Html", "Javadoc", "Jekyll")

    testProject.runner
      .addArguments("resolvableConfigurations", "-q")
      .build {
        output.invariantNewlines().asClue { allConfigurations ->

          val dokkatooConfigurations = allConfigurations.lines()
            .filter { it.contains("dokka", ignoreCase = true) }
            .mapNotNull { it.substringAfter("Configuration ", "").takeIf(String::isNotBlank) }

          dokkatooConfigurations.shouldContainExactlyInAnyOrder(
            buildList {
              add("dokkatoo")

              addAll(expectedFormats.map { "dokkatooModule$it" })
              addAll(expectedFormats.map { "dokkatooGeneratorClasspath$it" })
              addAll(expectedFormats.map { "dokkatooPlugin$it" })
              addAll(expectedFormats.map { "dokkatooPluginIntransitive$it" })
            }
          )

          withClue("Configuration dokka") {
            output.invariantNewlines() shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo
              |--------------------------------------------------
              |Fetch all Dokkatoo files from all configurations in other subprojects
              |
              |Attributes
              |    - org.jetbrains.dokka.dokkatoo.base = dokkatoo
              |
            """.trimMargin()
          }

          fun checkConfigurations(format: String) {
            val formatLowercase = format.lowercase()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatooGeneratorClasspath$format
              |--------------------------------------------------
              |Dokka Generator runtime classpath for $formatLowercase - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run.
              |
              |Attributes
              |    - org.jetbrains.dokka.dokkatoo.base       = dokkatoo
              |    - org.jetbrains.dokka.dokkatoo.category   = generator-classpath
              |    - org.jetbrains.dokka.dokkatoo.format     = $formatLowercase
              |    - org.gradle.category            = library
              |    - org.gradle.dependency.bundling = external
              |    - org.gradle.jvm.environment     = standard-jvm
              |    - org.gradle.libraryelements     = jar
              |    - org.gradle.usage               = java-runtime
              |Extended Configurations
              |    - dokkatooPlugin$format
              |
           """.trimMargin()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatooPlugin$format
              |--------------------------------------------------
              |Dokka Plugins classpath for $formatLowercase
              |
              |Attributes
              |    - org.jetbrains.dokka.dokkatoo.base       = dokkatoo
              |    - org.jetbrains.dokka.dokkatoo.category   = plugins-classpath
              |    - org.jetbrains.dokka.dokkatoo.format     = $formatLowercase
              |    - org.gradle.category            = library
              |    - org.gradle.dependency.bundling = external
              |    - org.gradle.jvm.environment     = standard-jvm
              |    - org.gradle.libraryelements     = jar
              |    - org.gradle.usage               = java-runtime
              |
            """.trimMargin()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatooPluginIntransitive$format
              |--------------------------------------------------
              |Dokka Plugins classpath for $formatLowercase - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration.
              |
              |Attributes
              |    - org.jetbrains.dokka.dokkatoo.base       = dokkatoo
              |    - org.jetbrains.dokka.dokkatoo.category   = plugins-classpath
              |    - org.jetbrains.dokka.dokkatoo.format     = $formatLowercase
              |    - org.gradle.category            = library
              |    - org.gradle.dependency.bundling = external
              |    - org.gradle.jvm.environment     = standard-jvm
              |    - org.gradle.libraryelements     = jar
              |    - org.gradle.usage               = java-runtime
              |Extended Configurations
              |    - dokkatooPlugin$format
              |
            """.trimMargin()
          }

          expectedFormats.forEach {
            checkConfigurations(it)
          }
        }
      }
  }
})
