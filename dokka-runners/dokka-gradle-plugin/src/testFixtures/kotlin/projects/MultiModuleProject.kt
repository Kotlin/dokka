package dev.adamko.dokkatoo.utils.projects

import dev.adamko.dokkatoo.internal.DokkatooConstants
import dev.adamko.dokkatoo.utils.*
import io.kotest.core.test.TestScope


fun TestScope.initMultiModuleProject(
  testName: String,
  config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {

  // get the FQN of the class that contains the test, so even though multiple
  // tests uses this project it's unlikely that the project dirs clash
  val baseDirName = testCase.descriptor.ids().first().value
    .substringAfter("dev.adamko.dokkatoo.") // drop the package name
    .replaceNonAlphaNumeric()

  return gradleKtsProjectTest("$baseDirName/multi-module-hello-goodbye/$testName") {

    settingsGradleKts += """
      |
      |include(":subproject-hello")
      |include(":subproject-goodbye")
      |
    """.trimMargin()

    buildGradleKts = """
      |plugins {
      |  // Kotlin plugin shouldn't be necessary here, but without it Dokka errors
      |  // with ClassNotFound KotlinPluginExtension... very weird
      |  kotlin("jvm") version embeddedKotlinVersion apply false
      |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
      |}
      |
      |dependencies {
      |  dokkatoo(project(":subproject-hello"))
      |  dokkatoo(project(":subproject-goodbye"))
      |}
      |
    """.trimMargin()

    dir("subproject-hello") {
      buildGradleKts = """
          |plugins {
          |  kotlin("jvm") version embeddedKotlinVersion
          |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
          |}
          |
        """.trimMargin()

      createKotlinFile(
        "src/main/kotlin/Hello.kt",
        """
          |package com.project.hello
          |
          |/** The Hello class */
          |class Hello {
          |    /** prints `Hello` to the console */  
          |    fun sayHello() = println("Hello")
          |}
          |
        """.trimMargin()
      )

      createKotlinFile("src/main/kotlin/HelloAgain.kt", "")
    }

    dir("subproject-goodbye") {

      buildGradleKts = """
          |plugins {
          |  kotlin("jvm") version embeddedKotlinVersion
          |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
          |}
          |
        """.trimMargin()

      createKotlinFile(
        "src/main/kotlin/Goodbye.kt",
        """
          |package com.project.goodbye
          |
          |/** The Goodbye class */
          |class Goodbye {
          |    /** prints a goodbye message to the console */  
          |    fun sayHello() = println("Goodbye!")
          |}
          |
        """.trimMargin()
      )
    }

    config()
  }
}
