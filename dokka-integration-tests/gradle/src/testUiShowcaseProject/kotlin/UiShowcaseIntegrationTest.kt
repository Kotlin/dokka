/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.it.TestOutputCopier
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.assertTrue

@EnabledOnOs(OS.MAC, disabledReason = "Contains KMP code for macOS")
class UiShowcaseIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {
    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/html") }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(LatestTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":jvm:dokkaGeneratePublicationHtml", ":kmp:dokkaGeneratePublicationHtml"
        ).buildRelaxed()

        result.shouldHaveTask(":jvm:dokkaGeneratePublicationHtml").shouldHaveOutcome(SUCCESS, FROM_CACHE)
        result.shouldHaveTask(":kmp:dokkaGeneratePublicationHtml").shouldHaveOutcome(SUCCESS, FROM_CACHE)

        val output = System.getenv("DOKKA_TEST_OUTPUT_PATH")!!
        listOf(
            "jvm" to File(projectDir, "jvm/build/dokka/html"),
            "kmp" to File(projectDir, "kmp/build/dokka/html"),
        ).forEach { (folder, input) ->
            val location = File(output).resolve(folder)
            println("Copying from ${input.absoluteFile} to ${location.absolutePath}")
            input.copyRecursively(location)
        }
    }

    override fun assertNoEmptyLinks(file: File) {
        // the versioning plugin's js script splits by '#', but it's not an empty link.
        // the page itself doesn't have any links, so it's fine to skip this file
        if (file.name.endsWith("not-found-version.html")) {
            return
        } else {
            super.assertNoEmptyLinks(file)
        }
    }
}
