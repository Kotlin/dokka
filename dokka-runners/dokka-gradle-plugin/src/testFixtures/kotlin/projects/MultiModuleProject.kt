/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils.projects

import io.kotest.core.test.TestScope
import org.jetbrains.dokka.gradle.internal.DokkatooConstants
import org.jetbrains.dokka.gradle.utils.*


fun TestScope.initMultiModuleProject(
    testName: String,
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {

    // get the FQN of the class that contains the test, so even though multiple
    // tests uses this project it's unlikely that the project dirs clash
    val baseDirName = testCase.descriptor.ids().first().value
        .substringAfter("org.jetbrains.dokka.gradle.") // drop the package name
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
      |  id("org.jetbrains.dokka") version "${DokkatooConstants.DOKKATOO_VERSION}"
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
          |  id("org.jetbrains.dokka") version "${DokkatooConstants.DOKKATOO_VERSION}"
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
          |  id("org.jetbrains.dokka") version "${DokkatooConstants.DOKKATOO_VERSION}"
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
