/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import org.jetbrains.dokka.gradle.internal.DokkaConstants
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.projects.defaultKgpTestVersion

class MemoryStressTest : FunSpec({
    context("when subprojects have 50 modules and uses single worker") {
        val project = setupProject(50, "500m")

        test("generate HTML publication") {
            project.runner
                .addArguments(
                    ":dokkaGenerate",
                    "--rerun-tasks",
                    "--stacktrace",
                    "--no-parallel",
                    "--max-workers=1" // reuse single worker all the time to reproduce the issue
                )
                .forwardOutput()
                .build {
                    // ensure build completed
                }
        }
    }
})

private fun setupProject(
    numberOfProjects: Int,
    metaspaceMemory: String
): GradleProjectTest = gradleKtsProjectTest("MemoryStressTest-$numberOfProjects") {
    val projectNames = List(numberOfProjects) { "subproject_$it" }
    settingsGradleKts += projectNames.joinToString("\n") { """include(":$it")""" }

    buildGradleKts = """
        |plugins {
        |  // Must apply KGP in the root project ensure consistent classpath, 
        |  // preventing issues like https://github.com/gradle/gradle/issues/17559 and https://github.com/gradle/gradle/issues/27218
        |  kotlin("jvm") version "$defaultKgpTestVersion" apply false
        |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
        |}
        |
        """.trimMargin()

    buildGradleKts += projectNames.joinToString(
        prefix = "dependencies {\n",
        separator = "\n",
        postfix = "\n}"
    ) { """  dokka(project(":$it"))""" }

    projectNames.forEach { projectName ->
        dir(projectName) {
            buildGradleKts = """
                |plugins {
                |  kotlin("jvm") version "$defaultKgpTestVersion"
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |}
                |
                |dokka {
                |  dokkaGeneratorIsolation.set(ProcessIsolation {
                |      maxHeapSize.set("1g")
                |      jvmArgs.add("-XX:MaxMetaspaceSize=$metaspaceMemory") // limit metaspace
                |  })
                |}
                |
                """.trimMargin()

            createKotlinFile(
                "src/main/kotlin/Hello.kt",
                """
                |package com.project.hello.${projectName}
                |
                |/** The Hello class */
                |class Hello {
                |    /** prints `Hello` to the console */  
                |    fun sayHello() = println("Hello")
                |}
                |
                """.trimMargin()
            )
        }
    }
}
