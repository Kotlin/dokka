/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.kotlin

import io.kotest.matchers.file.shouldContainFile
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

class IoBuildVersionsArgumentsProvider : ArgumentsProvider {
    private val buildVersions = BuildVersions.permutations(
        gradleVersions = listOf("8.9"), // should be consistent with Gradle version used in project gradle-wrapper.properties
        kotlinVersions = listOf("2.3.0") // not used, as we don't override it in external (git-based) projects
    )

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return buildVersions.stream().map { Arguments.of(it) }
    }
}

class IoGradleIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/html") }

    @BeforeTest
    override fun beforeEachTest() {
        prepareProjectFiles()
        copyAndApplyGitDiff(
            projectDir.toPath(),
            templateProjectDir.parent.resolve("io.diff"),
        )
        projectDir.toPath().updateProjectLocalMavenDir()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(IoBuildVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":dokkaGenerate",
            // disabled because:
            // w: [:kotlinx-io-okio:dokkaGenerateModuleHtml] Couldn't resolve link: [kotlinx.io.bytestring.ByteString] in file:////private/var/folders/t0/r4zzg8js2gnchztmlh57dyn80000gp/T/junit6370252363311766546/project/integration/kotlinx-io-okio/common/src/OkioAdapters.kt:153:18 (:kotlinx-io-okio/commonMain)
            // w: [:kotlinx-io-core:dokkaGenerateModuleHtml] Unknown annotation value `ExperimentalWasmInterop::class` in file:////private/var/folders/t0/r4zzg8js2gnchztmlh57dyn80000gp/T/junit6370252363311766546/project/core/wasmWasi/src/wasi/functions.kt:6:13
            "-Pdokka_it_failOnWarning=false",
        ).buildRelaxed()

        assertContains(
            setOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE),
            assertNotNull(result.task(":dokkaGeneratePublicationHtml")).outcome
        )

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.shouldContainFile("index.html")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            // assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
