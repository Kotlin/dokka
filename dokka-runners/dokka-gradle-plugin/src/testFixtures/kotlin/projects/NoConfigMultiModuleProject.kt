/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils.projects

import io.kotest.core.test.TestScope
import org.jetbrains.dokka.gradle.internal.DokkaConstants
import org.jetbrains.dokka.gradle.utils.*

/**
 * A simple multi-module project with no configuration
 * (so that it can be used in both DGPv1 and v2 modes).
 *
 * It's a multi-module project, and each subproject has slightly different Gradle plugins
 * to try and trigger Gradle classloader bugs so ensure that DGP can handle them.
 */
fun TestScope.initNoConfigMultiModuleProject(
    testName: String = testCase.name.testName,
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

        gradleProperties {
            dokka {
                pluginMode = null
            }
        }

        settingsGradleKts += """
            |
            |include(":subproject-one")
            |include(":subproject-two")
            |
            """.trimMargin()

        buildGradleKts = """
            |plugins {
            |  kotlin("jvm") version embeddedKotlinVersion apply false
            |  // important: don't register Dokka in the root project, because we _want_ the test to trigger a
            |  // Gradle classloader bug.
            |  //id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
            |}
            |
            """.trimMargin()

        dir("subproject-one") {
            buildGradleKts = """
                |plugins {
                |  kotlin("jvm") version embeddedKotlinVersion
                |  // important: Register different plugins here to make the buildscript classpath different,
                |  // because we _want_ the test to trigger a Gradle classloader bug.
                |  kotlin("plugin.serialization") version embeddedKotlinVersion
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |}
                |
                """.trimMargin()

            createKotlinFile(
                "src/main/kotlin/One.kt",
                """
                |package com.project.one
                |
                |/** `One` class */
                |class One {
                |    /** prints `One` to the console */  
                |    fun sayName() = println("One")
                |}
                |
                """.trimMargin()
            )
        }

        dir("subproject-two") {

            buildGradleKts = """
                |plugins {
                |  kotlin("jvm") version embeddedKotlinVersion
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |}
                |
                """.trimMargin()

            createKotlinFile(
                "src/main/kotlin/Two.kt",
                """
                |package com.project.two
                |
                |/** `Two` class */
                |class Two {
                |    /** prints `Two` to the console */  
                |    fun sayName() = println("Two")
                |}
                |
                """.trimMargin()
            )
        }

        config()
    }
}
