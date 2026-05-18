/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.assertTrue

class JsIRGradleIntegrationTest : AbstractGradleIntegrationTest() {
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val reactVersion = TestedVersions.KT_REACT_WRAPPER_MAPPING[buildVersions.kotlinVersion]
            ?: throw IllegalStateException("Unspecified version of react for kotlin " + buildVersions.kotlinVersion)
        val result = createGradleRunner(
            buildVersions,
            "-Preact_version=$reactVersion",
            "dokkaHtml",
        ).buildRelaxed()

        result.shouldHaveTask(":dokkaHtml").shouldHaveOutcome(SUCCESS, FROM_CACHE)

        val htmlOutputDir = File(projectDir, "build/dokka/html")
        assertTrue(htmlOutputDir.isDirectory, "Missing html output directory")

        assertTrue(
            htmlOutputDir.allHtmlFiles().count() > 0,
            "Expected html files in html output directory"
        )

        htmlOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoUnresolvedLinks(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
