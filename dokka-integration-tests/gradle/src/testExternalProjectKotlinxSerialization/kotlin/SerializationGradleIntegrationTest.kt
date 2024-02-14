/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.TestOutputCopier
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest
import org.jetbrains.dokka.it.gradle.BuildVersions
import org.jetbrains.dokka.it.gradle.OnlyDescriptors
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SerializationBuildVersionsArgumentsProvider : ArgumentsProvider {
    private val buildVersions = BuildVersions.permutations(
        gradleVersions = listOf("7.6.1"),
        kotlinVersions = listOf("1.9.0")
    )

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return buildVersions.stream().map { Arguments.of(it) }
    }
}

class SerializationGradleIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/htmlMultiModule") }

    @BeforeTest
    override fun beforeEachTest() {
        prepareProjectFiles()
        copyAndApplyGitDiff(
            projectDir.toPath(),
            templateProjectDir.parent.resolve("serialization.diff"),
        )
        projectDir.updateProjectLocalMavenDir()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SerializationBuildVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(buildVersions, ":dokkaHtmlMultiModule", "-i", "-s").buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtmlMultiModule")).outcome)

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
//            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
