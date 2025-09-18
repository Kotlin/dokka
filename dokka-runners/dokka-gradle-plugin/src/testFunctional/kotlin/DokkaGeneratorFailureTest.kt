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
        val project = createProject()

        test("expect failure message from checkers to be shown by default") {
            project.runner
                .addArguments(
                    ":dokkaGenerateModuleHtml",
                    "--rerun",
                )
                .buildAndFail {
                    output shouldContain "Pre-generation validity check failed: Some failure"
                }

            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--rerun",
                )
                .buildAndFail {
                    output shouldContain "Pre-generation validity check failed: Some failure"
                }
        }
    }
})

private fun createProject(): GradleProjectTest = gradleKtsProjectTest("dokka-generator-failure") {
    buildGradleKts = """
        |import org.jetbrains.dokka.gradle.tasks.*
        |
        |plugins {
        |    kotlin("jvm") version embeddedKotlinVersion
        |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
        |}
        |
        |dependencies {
        |    dokkaPlugin(project(":dokka-test-plugin"))
        |}
        |
        """.trimMargin()

    settingsGradleKts += """
        |include(":dokka-test-plugin")        
        |
        """.trimMargin()

    createKotlinFile("src/main/kotlin/Foo.kt", "class Foo")

    dir("dokka-test-plugin") {
        buildGradleKts = """
            |plugins {
            |    kotlin("jvm")
            |}
            |
            |dependencies {
            |    compileOnly("org.jetbrains.dokka:dokka-core:$DOKKA_VERSION")
            |    compileOnly("org.jetbrains.dokka:dokka-base:$DOKKA_VERSION")
            |}
            """.trimMargin()

        createKotlinFile(
            "src/main/kotlin/DokkaTestPlugin.kt", """
            |package dokkatest
            |
            |import org.jetbrains.dokka.*
            |import org.jetbrains.dokka.plugability.*
            |import org.jetbrains.dokka.validity.*
            |
            |class DokkaTestPlugin : DokkaPlugin() {
            |
            |    @DokkaPluginApiPreview
            |    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
            |
            |    val failingPreGenerationChecker by extending {
            |        CoreExtensions.preGenerationCheck providing ::FailingPreGenerationChecker
            |    }
            |}
            |
            |class FailingPreGenerationChecker(private val context: DokkaContext) : PreGenerationChecker {
            |    override fun invoke(): PreGenerationCheckerOutput = PreGenerationCheckerOutput(
            |        result = false,
            |        messages = listOf("Some failure")
            |    )
            |}
            |
            """.trimMargin()
        )

        createFile(
            "src/main/resources/META-INF/services/org.jetbrains.dokka.plugability.DokkaPlugin",
            "dokkatest.DokkaTestPlugin",
        )
    }
}
