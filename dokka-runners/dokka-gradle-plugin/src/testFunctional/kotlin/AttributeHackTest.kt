package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.internal.DokkatooConstants.DOKKATOO_VERSION
import dev.adamko.dokkatoo.utils.*
import io.kotest.core.spec.style.FunSpec


class AttributeHackTest : FunSpec({
  context("verify that Dokkatoo does not interfere with JAR Configurations") {

    val project = initProject()

    project.runner
      .addArguments(
        ":subproject-without-dokkatoo:printJarFileCoords",
        "--quiet",
        "--stacktrace",
        "--no-configuration-cache",
      )
      .forwardOutput()
      .build {
        test("resolving JARs from a Dokkatoo-enabled project should not contain Dokka plugin JARs") {
          output.shouldNotContainAnyOf(
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
      |include(":subproject-with-dokkatoo")
      |include(":subproject-without-dokkatoo")
      |
    """.trimMargin()

    dir("subproject-with-dokkatoo") {
      buildGradleKts = """
          |plugins {
          |  kotlin("multiplatform") version embeddedKotlinVersion
          |  id("dev.adamko.dokkatoo-html") version "$DOKKATOO_VERSION"
          |}
          |
          |kotlin {
          |  jvm()
          |}
          |
        """.trimMargin()
    }

    dir("subproject-without-dokkatoo") {

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
        |  jarFiles(project(":subproject-with-dokkatoo"))
        |}
        |
        |val printJarFileCoords by tasks.registering {
        |  val fileCoords = jarFilesResolver.incoming.artifacts.resolvedArtifacts.map { artifacts ->
        |    artifacts.map { it.id.componentIdentifier.displayName }
        |  }
        |  inputs.files(jarFilesResolver).withPropertyName("jarFilesResolver")
        |  doLast {
        |    println(fileCoords.get().joinToString("\n"))
        |  }
        |}
        |
        """.trimMargin()
    }

    config()
  }
}
