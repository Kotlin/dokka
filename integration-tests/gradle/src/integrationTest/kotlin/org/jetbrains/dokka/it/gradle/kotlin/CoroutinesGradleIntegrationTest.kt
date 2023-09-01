/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.TestOutputCopier
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest
import org.jetbrains.dokka.it.gradle.BuildVersions
import org.jetbrains.dokka.it.gradle.TestedVersionsWithK2SwitcherArgumentsProvider
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val buildVersions = BuildVersions.permutations(
    gradleVersions = listOf("7.4.2"),
    kotlinVersions = listOf("1.8.10")
)

internal class CoroutinesBuildVersionsArgumentsProvider :
    TestedVersionsWithK2SwitcherArgumentsProvider(buildVersions)
class CoroutinesGradleIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/htmlMultiModule") }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "coroutines/kotlinx-coroutines")
        templateProjectDir.listFiles().orEmpty()
            .forEach { topLevelFile -> topLevelFile.copyRecursively(File(projectDir, topLevelFile.name)) }

        copyAndApplyGitDiff(File("projects", "coroutines/coroutines.diff"))
    }

    @ParameterizedTest(name = "{0} {1}")
    @ArgumentsSource(CoroutinesBuildVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions, extraParameter: String) {
        val result = createGradleRunner(
            buildVersions,
            ":dokkaHtmlMultiModule", "-i", "-s", extraParameter,
            jvmArgs = listOf("-Xmx2G", "-XX:MaxMetaspaceSize=500m")
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtmlMultiModule")).outcome)

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
//            assertContainsNoErrorClass(file)
//            assertNoUnresolvedLinks(file)
//            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }
    }
}
