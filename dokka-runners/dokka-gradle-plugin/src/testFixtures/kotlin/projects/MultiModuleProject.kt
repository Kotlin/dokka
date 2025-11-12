/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils.projects

import io.kotest.core.test.TestScope
import org.jetbrains.dokka.gradle.internal.DokkaConstants
import org.jetbrains.dokka.gradle.utils.*


fun TestScope.initMultiModuleProject(
    testName: String,
    rootProjectName: String? = null,
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {

    // get the FQN of the class that contains the test, so even though multiple
    // tests uses this project it's unlikely that the project dirs clash
    val baseDirName = testCase.descriptor.ids().first().value
        .substringAfter("org.jetbrains.dokka.gradle.") // drop the package name
        .replaceNonAlphaNumeric()

    return gradleKtsProjectTest(
        projectLocation = "$baseDirName/multi-module-hello-goodbye/$testName",
        rootProjectName = rootProjectName,
    ) {

        settingsGradleKts += """
            |
            |include(":subproject-hello")
            |include(":subproject-goodbye")
            |
            """.trimMargin()

        buildGradleKts = """
            |plugins {
            |  // Must apply KGP in the root project ensure consistent classpath, 
            |  // preventing issues like https://github.com/gradle/gradle/issues/17559 and https://github.com/gradle/gradle/issues/27218
            |  kotlin("jvm") version embeddedKotlinVersion apply false
            |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
            |  id("org.jetbrains.dokka-javadoc") version "${DokkaConstants.DOKKA_VERSION}"
            |}
            |
            |dependencies {
            |  dokka(project(":subproject-hello"))
            |  dokka(project(":subproject-goodbye"))
            |}
            |
            """.trimMargin()

        dir("subproject-hello") {
            buildGradleKts = """
                |plugins {
                |  kotlin("jvm") version embeddedKotlinVersion
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |  id("org.jetbrains.dokka-javadoc") version "${DokkaConstants.DOKKA_VERSION}"
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
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |  id("org.jetbrains.dokka-javadoc") version "${DokkaConstants.DOKKA_VERSION}"
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
