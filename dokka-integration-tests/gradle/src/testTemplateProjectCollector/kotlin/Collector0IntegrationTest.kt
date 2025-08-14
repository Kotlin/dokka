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

class Collector0IntegrationTest : AbstractGradleIntegrationTest() {

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":moduleA:dokkaHtmlCollector",
            ":moduleA:dokkaJavadocCollector",
            ":moduleA:dokkaGfmCollector",
            ":moduleA:dokkaJekyllCollector",
            jvmArgs = listOf(
                "-Xmx1G", "-XX:MaxMetaspaceSize=800m",
                "-XX:SoftRefLRUPolicyMSPerMB=10" // to free up the metaspace on JVM 8, see https://youtrack.jetbrains.com/issue/KT-55831/
            )
        ).buildRelaxed()

        result.shouldHaveTask(":moduleA:dokkaHtmlCollector").shouldHaveOutcome(SUCCESS, FROM_CACHE)
        result.shouldHaveTask(":moduleA:dokkaJavadocCollector").shouldHaveOutcome(SUCCESS, FROM_CACHE)
        result.shouldHaveTask(":moduleA:dokkaGfmCollector").shouldHaveOutcome(SUCCESS, FROM_CACHE)
        result.shouldHaveTask(":moduleA:dokkaJekyllCollector").shouldHaveOutcome(SUCCESS, FROM_CACHE)

        File(projectDir, "moduleA/build/dokka/htmlCollector").assertHtmlOutputDir()
        File(projectDir, "moduleA/build/dokka/javadocCollector").assertJavadocOutputDir()
        File(projectDir, "moduleA/build/dokka/gfmCollector").assertGfmOutputDir()
        File(projectDir, "moduleA/build/dokka/jekyllCollector").assertJekyllOutputDir()
    }

    private fun File.assertHtmlOutputDir() {
        assertTrue(isDirectory, "Missing dokka htmlCollector output directory")
        allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }

        assertTrue(
            allHtmlFiles().any { file -> "moduleB" in file.readText() },
            "Expected moduleB to be present in html"
        )

        assertTrue(
            allHtmlFiles().any { file -> "moduleC" in file.readText() },
            "Expected moduleC to be present in html"
        )
    }

    private fun File.assertJavadocOutputDir() {
        assertTrue(isDirectory, "Missing dokka javadocCollector output directory")
    }

    private fun File.assertJekyllOutputDir() {
        assertTrue(isDirectory, "Missing dokka jekyllCollector output directory")
    }

    private fun File.assertGfmOutputDir() {
        assertTrue(isDirectory, "Missing dokka gfmCollector output directory")
    }
}
