/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldNotBeBlank
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*

/**
 * Test Dokka creates Configurations that do not interfere with
 * the dependency resolution of other JVM plugins.
 */
class DokkaDependenciesCompatibilityTest : FunSpec({
    context("verify that DGP does not interfere with resolving JAR Configurations") {

        val project = initProject()

        project.runner
            .addArguments(
                ":subproject-without-dokka:printJarFileCoords",
                "--quiet",
                "--stacktrace",
                "--no-configuration-cache",
            )
            .forwardOutput()
            .build {
                test("resolving JARs from a Dokka-enabled project should not contain Dokka plugin JARs") {

                    val fileCoords = output
                        .substringAfter("--- fileCoords ---", "")
                        .substringBefore("--- fileCoords ---", "")

                    fileCoords.shouldNotBeBlank()

                    fileCoords.shouldContainAll(
                        "project :subproject-with-dokka",
                        "org.jetbrains.kotlin:kotlin-stdlib",
                    )

                    fileCoords.shouldNotContainAnyOf(
                        "org.jetbrains.dokka",
                        "all-modules-page-plugin",
                    )
                }
            }
    }
})


private fun initProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest("attribute-hack-test") {

        settingsGradleKts += """
            |
            |include(":subproject-with-dokka")
            |include(":subproject-without-dokka")
            |
            """.trimMargin()

        dir("subproject-with-dokka") {
            buildGradleKts = """
                |plugins {
                |  kotlin("multiplatform") version embeddedKotlinVersion
                |  id("org.jetbrains.dokka") version "$DOKKA_VERSION"
                |}
                |
                |kotlin {
                |  jvm()
                |}
                |
                """.trimMargin()
        }

        dir("subproject-without-dokka") {

            buildGradleKts = """
                |import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
                |import org.gradle.api.attributes.Category.LIBRARY
                |import org.gradle.api.attributes.LibraryElements.JAR
                |import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
                |import org.gradle.api.attributes.Usage.JAVA_RUNTIME
                |import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
                |import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
                |import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
                |
                |plugins {
                |  `java-library`
                |}
                |
                |val jarFiles: Configuration by configurations.creating {
                |  isCanBeResolved = false
                |  isCanBeConsumed = false
                |  isCanBeDeclared = true
                |}
                |
                |val jarFilesResolver: Configuration by configurations.creating {
                |  isCanBeResolved = true
                |  isCanBeConsumed = false
                |  isCanBeDeclared = false
                |  extendsFrom(jarFiles)
                |  attributes {
                |    //attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
                |    //attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
                |    //attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
                |    //attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
                |    //attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
                |  }
                |}
                |
                |dependencies {
                |  jarFiles(project(":subproject-with-dokka"))
                |}
                |
                |val printJarFileCoords by tasks.registering {
                |  val fileCoords = jarFilesResolver.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                |    artifacts.map { it.id.componentIdentifier.displayName }
                |  }
                |  inputs.files(jarFilesResolver).withPropertyName("jarFilesResolver")
                |  doLast {
                |    println("--- fileCoords ---")
                |    println(fileCoords.get().joinToString("\n"))
                |    println("--- fileCoords ---")
                |  }
                |}
                |
                """.trimMargin()
        }

        config()
    }
}
