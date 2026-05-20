/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.TestOutputCopier
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest
import org.jetbrains.dokka.it.gradle.BuildVersions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.BeforeTest
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollectionsImmutableBuildVersionsArgumentsProvider : ArgumentsProvider {
    private val buildVersions = BuildVersions.permutations(
        gradleVersions = listOf("8.10"), // should be consistent with Gradle version used in project gradle-wrapper.properties
        kotlinVersions = listOf("2.3.0") // not used, as we don't override it in external (git-based) projects
    )

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return buildVersions.stream().map { Arguments.of(it) }
    }
}

class CollectionsImmutableGradleIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {

    override val projectOutputLocation: File by lazy { File(projectDir, "core/build/dokka/html") }

    @BeforeTest
    override fun beforeEachTest() {
        prepareProjectFiles()
        copyAndApplyGitDiff(
            projectDir.toPath(),
            templateProjectDir.parent.resolve("collectionsImmutable.diff"),
        )
        projectDir.toPath().updateProjectLocalMavenDir()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CollectionsImmutableBuildVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":kotlinx-collections-immutable:dokkaGenerate",
            "-Pdokka_it_failOnWarning=true"
        ).buildRelaxed()

        assertContains(
            setOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE),
            assertNotNull(result.task(":kotlinx-collections-immutable:dokkaGeneratePublicationHtml")).outcome
        )

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
