/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.plugins

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.utils.enableV2Plugin

class DokkaVersioningPluginParametersTest : FunSpec({

    val project = ProjectBuilder.builder().build()
        .enableV2Plugin()
    project.plugins.apply(type = DokkaPlugin::class)

    fun TestScope.versioningPluginParams(
        configure: DokkaVersioningPluginParameters.() -> Unit = {}
    ): DokkaVersioningPluginParameters =
        project.extensions
            .getByType<DokkaExtension>()
            .pluginsConfiguration
            .create<DokkaVersioningPluginParameters>(testCase.name.testName, configure)


    context("when params have default convention values") {
        val params = versioningPluginParams {
            // no configuration, default values
        }

        test("expect version is null") {
            params.version.orNull.shouldBeNull()
        }
        test("expect versionsOrdering is empty list") {
            params.versionsOrdering.orNull.shouldBeEmpty()
        }
        test("expect olderVersionsDir is null") {
            params.olderVersionsDir.orNull.shouldBeNull()
        }
        test("expect olderVersions is empty") {
            params.olderVersions.shouldBeEmpty()
        }
        test("expect olderVersionsDirName is 'older'") {
            params.olderVersionsDirName.orNull shouldBe "older"
        }
        test("expect renderVersionsNavigationOnAllPages is true") {
            params.renderVersionsNavigationOnAllPages.orNull shouldBe true
        }

        test("expect correct JSON") {
            params.jsonEncode() shouldEqualJson /* language=JSON */ """
                |{
                |  "olderVersions": [],
                |  "olderVersionsDirName": "older",
                |  "renderVersionsNavigationOnAllPages": true
                |}
            """.trimMargin()
        }
    }

    context("when params are set, expect correct JSON") {
        val params = versioningPluginParams {
            // no configuration, default values
            version.set("x.y.z")
            versionsOrdering.set(listOf("a.b.c", "x.y.z", "1.2.3"))
            olderVersionsDir.set(project.layout.buildDirectory.dir("older-versions-dir"))
            olderVersions.from(project.layout.buildDirectory.dir("older-versions"))
            olderVersionsDirName.set("versions")
            renderVersionsNavigationOnAllPages.set(false)
        }

        test("expect correct JSON") {
            val buildDir = project.layout.buildDirectory.get().asFile.invariantSeparatorsPath
            params.jsonEncode() shouldEqualJson /* language=JSON */ """
                |{
                |  "version": "x.y.z",
                |  "versionsOrdering": [
                |    "a.b.c",
                |    "x.y.z",
                |    "1.2.3"
                |  ],
                |  "olderVersionsDir": "${buildDir}/older-versions-dir",
                |  "olderVersions": [
                |    "${buildDir}/older-versions"
                |  ],
                |  "olderVersionsDirName": "versions",
                |  "renderVersionsNavigationOnAllPages": false
                |}
            """.trimMargin()
        }
    }


    context("when versionsOrdering are set as an empty list") {
        val params = versioningPluginParams {
            versionsOrdering.set(emptyList())
        }

        test("expect versionsOrdering is null") {
            params.versionsOrdering.orNull.shouldBeEmpty()
        }

        test("expect versionsOrdering not present in JSON") {
            params.jsonEncode() shouldEqualJson /* language=JSON */ """
                |{
                |  "olderVersions": [],
                |  "olderVersionsDirName": "older",
                |  "renderVersionsNavigationOnAllPages": true
                |}
            """.trimMargin()
        }
    }
})
