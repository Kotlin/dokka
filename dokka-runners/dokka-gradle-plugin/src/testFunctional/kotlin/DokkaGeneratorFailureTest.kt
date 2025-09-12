/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*

class DokkaGeneratorFailureTest : FunSpec({
    context("DokkaGenerator failure:") {
        val project = gradleKtsProjectTest("dokka-generator-failure") {
            buildGradleKts = """
                |plugins {
                |    kotlin("jvm") version embeddedKotlinVersion
                |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
                |}
                |
                |dokka {
                |  dokkaSourceSets.configureEach {
                |    suppress = false // to enable documentation for `test` source set
                |    sourceRoots.from("src/shared/kotlin") // causes an error in Dokka generator checker
                |  }
                |}
                |
                """.trimMargin()

            createKotlinFile("src/main/kotlin/Main.kt", "class Main")
            createKotlinFile("src/test/kotlin/Test.kt", "class Test")
            createKotlinFile("src/shared/kotlin/Shared.kt", "class Shared")
        }

        test("expect failure message from checkers to be shown by default") {
            project.runner
                .addArguments(
                    ":dokkaGenerateModuleHtml",
                    "--rerun",
                )
                .buildAndFail {
                    output shouldContain "Pre-generation validity check failed"
                    output shouldContain "Source sets 'java' and 'javaTest' have the common source roots"
                }

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun",
                )
                .buildAndFail {
                    output shouldContain "Pre-generation validity check failed"
                    output shouldContain "Source sets 'java' and 'javaTest' have the common source roots"
                }
        }
    }
})
