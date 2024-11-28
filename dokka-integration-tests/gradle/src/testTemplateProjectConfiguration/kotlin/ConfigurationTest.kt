/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * Tests for Dokka's configuration options of the Gradle runner.
 *
 * Options can be checked to work in combination with each other:
 * for instance, you can check that `reportUndocumented` and `failOnWarning`
 * work in synergy when both set to true.
 *
 * Configuration options can be passed as project properties using Gradle CLI arguments.
 * For example, passing `-Pname=value` to Gradle will create a project-wide property with
 * key `name` and value `value`, which you can use to set the corresponding option's value
 * using Dokka's configuration DSL.
 */
class ConfigurationTest : AbstractGradleIntegrationTest() {

    /**
     * The test project contains some undocumented declarations, so if both `reportUndocumented`
     * and `failOnWarning` are enabled - it should fail
     */
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(LatestTestedVersionsArgumentsProvider::class)
    fun `should fail with DokkaException and readable message if failOnWarning is triggered`(
        buildVersions: BuildVersions
    ) {
        val result = createGradleRunner(
            buildVersions,
            "-Preport_undocumented=true",
            "-Pfail_on_warning=true",
            "dokkaHtml"
        ).buildAndFail()

        result.shouldHaveTask(":dokkaHtml").shouldHaveOutcome(FAILED)

        result.output.contains("> Task :dokkaHtml FAILED")
        result.output.contains(
            """
               FAILURE: Build failed with an exception\\.
               
               \* What went wrong:
               Execution failed for task ':dokkaHtml'\\.
               > Failed with warningCount=\d and errorCount=\d
           """.trimIndent().toRegex()
        )

        result.output.contains(
            "Caused by: org\\.jetbrains\\.dokka\\.DokkaException: Failed with warningCount=\\d and errorCount=\\d".toRegex()
        )
    }
}
