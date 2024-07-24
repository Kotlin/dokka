/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.internal.DokkatooConstants.DOKKATOO_VERSION
import org.jetbrains.dokka.gradle.utils.*

class DokkatooPluginFunctionalTest : FunSpec({
    val testProject = gradleKtsProjectTest("DokkatooPluginFunctionalTest") {

        buildGradleKts = """
      |plugins {
      |  id("org.jetbrains.dokka") version "$DOKKATOO_VERSION"
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
            "prepareDokkatooModuleDescriptorGfm"     to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for gfm",
            "prepareDokkatooModuleDescriptorHtml"    to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for html",
            "prepareDokkatooModuleDescriptorJavadoc" to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for javadoc",
            "prepareDokkatooModuleDescriptorJekyll"  to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for jekyll",
            //@formatter:on
                    )
                }
            }
    }

    test("expect Dokka Plugin creates Dokka outgoing variants") {
        testProject.runner
            .addArguments("outgoingVariants", "-q")
            .build {
                val variants = output.invariantNewlines().replace('\\', '/')

                val dokkatooVariants = variants.lines()
                    .filter { it.startsWith("Variant ") && it.contains("dokka", ignoreCase = true) }
                    .mapNotNull { it.substringAfter("Variant ", "").ifBlank { null } }

                dokkatooVariants.shouldContainExactlyInAnyOrder(
                    expectedFormats.flatMap {
                        listOf(
                            "dokkatoo${it}ModuleOutputDirectoriesConsumable",
                            "dokkatoo${it}PublicationPluginClasspathApiOnlyConsumable",
                        )
                    }
                )

                fun checkVariant(format: String) {
                    @Suppress("LocalVariableName")
                    val Format = format.uppercaseFirstChar()

                    variants shouldContain /* language=text */ """
            |--------------------------------------------------
            |Variant dokkatoo${Format}ModuleOutputDirectoriesConsumable
            |--------------------------------------------------
            |Provides Dokkatoo $format ModuleOutputDirectories files for consumption by other subprojects.
            |
            |Capabilities
            |    - :test:unspecified (default capability)
            |Attributes
            |    - org.gradle.usage                     = org.jetbrains.dokka
            |    - org.jetbrains.dokka.format           = $format
            |    - org.jetbrains.dokka.module-component = ModuleOutputDirectories
            |Artifacts
            |    - build/dokka-module/$format (artifactType = dokka-module-directory)
          """.trimMargin()
                }

                checkVariant("gfm")
                checkVariant("html")
                checkVariant("javadoc")
                checkVariant("jekyll")
            }
    }

    xtest("expect Dokka Plugin creates Dokka resolvable configurations") {
        // disabled because Gradle is bugged https://github.com/gradle/gradle/issues/28733

        testProject.runner
            .addArguments("resolvableConfigurations", "-q")
            .build {
                output.invariantNewlines().asClue { allConfigurations ->

                    val dokkatooConfigurations = allConfigurations.lines()
                        .filter { it.contains("dokka", ignoreCase = true) }
                        .mapNotNull { it.substringAfter("Configuration ", "").takeIf(String::isNotBlank) }

                    dokkatooConfigurations.shouldContainExactlyInAnyOrder(
                        buildSet {
                            addAll(expectedFormats.map { "dokkatoo${it}GeneratorClasspathResolver" })
                            addAll(expectedFormats.map { "dokkatoo${it}ModuleOutputDirectoriesResolver" })
                            addAll(expectedFormats.map { "dokkatoo${it}PluginsClasspathIntransitiveResolver" })
                            addAll(expectedFormats.map { "dokkatoo${it}PublicationPluginClasspathResolver" })
                        }
                    )

                    fun checkConfigurations(
                        @Suppress("LocalVariableName")
                        Format: String
                    ) {
                        val format = Format.lowercase()

                        allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}GeneratorClasspathResolver
              |--------------------------------------------------
              |Dokka Generator runtime classpath for $format - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run.
              |
              |Attributes
              |    - org.gradle.category            = Dokkatoo~library
              |    - org.gradle.dependency.bundling = Dokkatoo~external
              |    - org.gradle.jvm.environment     = Dokkatoo~standard-jvm
              |    - org.gradle.libraryelements     = Dokkatoo~jar
              |    - org.gradle.usage               = Dokkatoo~java-runtime
              |    - org.jetbrains.dokka.classpath  = dokka-generator
              |    - org.jetbrains.dokka.format     = $format
              |Extended Configurations
              |    - dokkatoo${Format}GeneratorClasspath
           """.trimMargin()

                        allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}PluginsClasspathIntransitiveResolver
              |--------------------------------------------------
              |Resolves Dokka Plugins classpath for $format - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration.
              |
              |Attributes
              |    - org.gradle.category            = Dokkatoo~library
              |    - org.gradle.dependency.bundling = Dokkatoo~external
              |    - org.gradle.jvm.environment     = Dokkatoo~standard-jvm
              |    - org.gradle.libraryelements     = Dokkatoo~jar
              |    - org.gradle.usage               = Dokkatoo~java-runtime
              |    - org.jetbrains.dokka.classpath  = dokka-plugins
              |    - org.jetbrains.dokka.format     = $format
              |Extended Configurations
              |    - dokkatooPlugin${Format}
           """.trimMargin()

                        allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}ModuleOutputDirectoriesResolver
              |--------------------------------------------------
              |Resolves Dokkatoo $format ModuleOutputDirectories files.
              |
              |Attributes
              |    - org.gradle.usage                     = org.jetbrains.dokka
              |    - org.jetbrains.dokka.format           = $format
              |    - org.jetbrains.dokka.module-component = ModuleOutputDirectories
              |Extended Configurations
              |    - dokkatoo
            """.trimMargin()
                    }

                    expectedFormats.forEach {
                        checkConfigurations(it)
                    }
                }
            }
    }
}) {
    companion object {
        private val expectedFormats = listOf("Gfm", "Html", "Javadoc", "Jekyll")
    }
}
