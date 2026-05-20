/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.gradle.junit.DokkaGradleProjectRunner
import org.jetbrains.dokka.it.gradle.junit.TestedVersions
import kotlin.test.assertContains
import kotlin.test.assertNotNull


/**
 * Assert that the [BuildResult] has a task in the task graph.
 *
 * @returns the task, if it exists.
 */
fun BuildResult.shouldHaveTask(taskPath: String): BuildTask {
    val actual = task(taskPath)
    assertNotNull(actual) {
        "Could not find task $taskPath in BuildResult. All tasks: ${tasks.map { it.path }}"
    }
    return actual
}

/**
 * Assert that the [outcome][TaskOutcome] of a [BuildTask] is any of [expectedOutcomes].
 */
fun BuildTask.shouldHaveOutcome(
    vararg expectedOutcomes: TaskOutcome
) {
    require(expectedOutcomes.isNotEmpty()) {
        "Invalid assertion. Must have at least 1 expected TaskOutcome, but got none."
    }

    val message = buildString {
        append("Expected that task $path outcome was")

        if (expectedOutcomes.size == 1) {
            append(" ${expectedOutcomes.single()}, ")
        } else {
            append(" any of ${expectedOutcomes.toList()}, ")
        }
        append("but was actually $outcome")
    }

    assertContains(expectedOutcomes.toList(), outcome, message)
}


fun testConfigurationCacheResult(
    project: DokkaGradleProjectRunner,
    testedVersions: TestedVersions,
) {
    val ccReport = loadConfigurationCacheReportData(projectDir = project.projectDir)

    withClue("should have no CC problems") {
        ccReport.totalProblemCount shouldBe 0
    }

    if (testedVersions.gradle.major > 9) {
        withClue("Dokka should only use `org.jetbrains.dokka` Gradle properties for CC") {
            val dokkaDiagnostics =
                ccReport.diagnostics
                    // find diagnostics of Dokka's CC inputs
                    .filter { diag ->
                        diag.trace.any { "org.jetbrains.dokka" in it.location.orEmpty() }
                    }

            dokkaDiagnostics
                .shouldNotBeEmpty()
                // verify all Dokka's CC inputs are namespaced with `org.jetbrains.dokka`.
                .shouldForAll { diag ->
                    val inputNames = diag.input.mapNotNull { it.name }
                    inputNames.shouldForAll { inputName ->
                        inputName.shouldStartWith("org.jetbrains.dokka.")
                    }
                }
        }
    }
}
