/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.assertTrue

// SubclassOptInRequired used in test is supported starting from 1.8
internal class MultiModuleMultiplatformTestedVersionsArgumentsProvider : AllSupportedTestedVersionsArgumentsProvider() {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return super.provideArguments(context).filter {
            val buildVersions = it.get().single() as BuildVersions
            buildVersions.kotlinVersion >= "1.8.20"
        }
    }
}

class MultiModuleMultiplatformIntegrationTest : AbstractGradleIntegrationTest() {

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(MultiModuleMultiplatformTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":dokkaHtmlMultiModule",
        ).buildRelaxed()

        result.shouldHaveTask(":dokkaHtmlMultiModule").shouldHaveOutcome(SUCCESS, FROM_CACHE)
        result.shouldHaveTask(":first:dokkaHtmlPartial").shouldHaveOutcome(SUCCESS, FROM_CACHE)
        result.shouldHaveTask(":second:dokkaHtmlPartial").shouldHaveOutcome(SUCCESS, FROM_CACHE)

        val outputDir = File(projectDir, "build/dokka/htmlMultiModule")
        assertTrue(outputDir.isDirectory, "Missing dokka output directory")
        assertTrue(
            outputDir.allHtmlFiles().any(),
            "Expected at least one html file being generated"
        )

        outputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }

        val modulesFile = File(outputDir, "index.html")
        assertTrue(modulesFile.isFile, "Missing index.html file")

        val modulesFileText = modulesFile.readText()
        assertTrue(
            "first" in modulesFileText,
            "Expected first being mentioned in -modules.html"
        )
        assertTrue(
            "second" in modulesFileText,
            "Expected second being mentioned in -modules.html"
        )
    }
}
