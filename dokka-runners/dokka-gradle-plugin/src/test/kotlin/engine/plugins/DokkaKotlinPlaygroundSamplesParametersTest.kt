/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.engine.plugins

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.utils.enableV2Plugin

class DokkaKotlinPlaygroundSamplesParametersTest : FunSpec({

    val project = ProjectBuilder.builder().build()
        .enableV2Plugin()
    project.plugins.apply(type = DokkaPlugin::class)

    fun TestScope.kotlinPlaygroundSamplesPluginParams(
        configure: DokkaKotlinPlaygroundSamplesParameters.() -> Unit = {}
    ): DokkaKotlinPlaygroundSamplesParameters =
        project.extensions
            .getByType<DokkaExtension>()
            .pluginsConfiguration
            .create<DokkaKotlinPlaygroundSamplesParameters>(testCase.name.testName, configure)


    context("when no params are provided") {
        val params = kotlinPlaygroundSamplesPluginParams {
            // no configuration
        }

        test("kotlinPlaygroundScript is null") {
            params.kotlinPlaygroundScript.orNull.shouldBeNull()
        }

        test("kotlinPlaygroundServer is null") {
            params.kotlinPlaygroundServer.orNull.shouldBeNull()
        }

        test("JSON is empty") {
            params.jsonEncode() shouldEqualJson "{}"
        }
    }

    context("when only kotlinPlaygroundScript is provided") {
        val kotlinPlaygroundScriptUrl = "https://customKotlinPlaygroundScript/example.js"
        val params = kotlinPlaygroundSamplesPluginParams {
            kotlinPlaygroundScript.set(kotlinPlaygroundScriptUrl)
        }

        test("kotlinPlaygroundScript is correct") {
            params.kotlinPlaygroundScript.orNull shouldBe kotlinPlaygroundScriptUrl
        }

        test("kotlinPlaygroundServer is null") {
            params.kotlinPlaygroundServer.orNull.shouldBeNull()
        }

        test("expect correct JSON") {
            params.jsonEncode() shouldEqualJson """
                |{
                |  "kotlinPlaygroundScript": "$kotlinPlaygroundScriptUrl"
                |}
            """.trimMargin()
        }
    }

    context("when only kotlinPlaygroundServer is provided") {
        val kotlinPlaygroundServerUrl = "https://kotlinPlaygroundServer.example.com/"
        val params = kotlinPlaygroundSamplesPluginParams {
            kotlinPlaygroundServer.set(kotlinPlaygroundServerUrl)
        }

        test("kotlinPlaygroundScript is null") {
            params.kotlinPlaygroundScript.orNull.shouldBeNull()
        }

        test("kotlinPlaygroundServer is correct") {
            params.kotlinPlaygroundServer.orNull shouldBe kotlinPlaygroundServerUrl
        }

        test("expect correct JSON") {
            params.jsonEncode() shouldEqualJson """
                |{
                |  "kotlinPlaygroundServer": "$kotlinPlaygroundServerUrl"
                |}
            """.trimMargin()
        }
    }

    context("when both kotlinPlaygroundScript and kotlinPlaygroundServer are provided") {
        val kotlinPlaygroundScriptUrl = "https://customKotlinPlaygroundScript/example.js"
        val kotlinPlaygroundServerUrl = "https://kotlinPlaygroundServer.example.com/"

        val params = kotlinPlaygroundSamplesPluginParams {
            kotlinPlaygroundScript.set(kotlinPlaygroundScriptUrl)
            kotlinPlaygroundServer.set(kotlinPlaygroundServerUrl)
        }

        test("kotlinPlaygroundScript is correct") {
            params.kotlinPlaygroundScript.orNull shouldBe kotlinPlaygroundScriptUrl
        }

        test("kotlinPlaygroundServer is correct") {
            params.kotlinPlaygroundServer.orNull shouldBe kotlinPlaygroundServerUrl
        }

        test("expect correct JSON") {
            params.jsonEncode() shouldEqualJson """
                |{
                |  "kotlinPlaygroundScript": "$kotlinPlaygroundScriptUrl",
                |  "kotlinPlaygroundServer": "$kotlinPlaygroundServerUrl"
                |}
            """.trimMargin()
        }
    }
})
