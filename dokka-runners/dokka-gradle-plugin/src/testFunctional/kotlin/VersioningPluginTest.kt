/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.paths.shouldNotExist
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*

class VersioningPluginTest : FunSpec({

    val testProject = gradleKtsProjectTest("versioning-plugin-test-project") {
        buildGradleKts = """
            |plugins {
            |  kotlin("jvm") version embeddedKotlinVersion
            |  id("org.jetbrains.dokka") version "$DOKKA_VERSION"
            |}
            |
            |dependencies {
            |    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin")
            |}
            |
            |val currentVersion = "1.0"
            |val previousVersionsDirectory: Directory = layout.projectDirectory.dir("previousDocVersions")
            |
            |dokka {
            |    moduleName.set("versioning-plugin-test-project")
            |    pluginsConfiguration {
            |        versioning {
            |            version = currentVersion
            |            olderVersionsDir = previousVersionsDirectory
            |        }
            |    }
            |}
            |
            |""".trimMargin()
    }

    test("verify olderVersionsDir does not need to exist") {
        // This test checks Gradle does not fail when olderVersionsDir does not exist.
        // https://github.com/gradle/gradle/issues/2016#issuecomment-965126604

        val versioningDir = testProject.file("previousDocVersions")
        versioningDir.shouldNotExist()

        testProject.runner
            .addArguments(":dokkaGenerate")
            .build {
                shouldHaveRunTask(":dokkaGenerate")
            }
    }
})
