/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.shouldNotHaveRunTask
import org.jetbrains.dokka.it.gradle.junit.*
import org.jetbrains.dokka.it.gradle.junit.TestedVersions
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Integration test for the `it-kotlin-jvm` project.
 */
@TestsKotlinJvm
@TestsDGPv2
class KotlinJvmIT {

    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-jvm")
    fun `Dokka tasks should be build cacheable`(
        project: DokkaGradleProjectRunner,
    ) {
        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--build-cache",
            )
            .build {
                withClue("expect dokkaGenerate runs successfully") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)
                    shouldHaveTask(":dokkaGeneratePublicationHtml").shouldHaveOutcome(FROM_CACHE, SUCCESS)
                }
                withClue("only one project is documented, so expect no Dokka module generation") {
                    shouldNotHaveRunTask(":dokkaGenerateModuleHtml")
                }
            }

        project.runner
            .addArguments(
                ":clean",
                "--build-cache",
            ).build {
                withClue("expect clean runs successfully") {
                    shouldHaveTask(":clean").shouldHaveOutcome(SUCCESS)
                }
            }

        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--build-cache",
            )
            .build {
                withClue("expect dokkaGenerate lifecycle task is up-to-date") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE)
                }
                withClue("expect dokkaGenerate* work tasks are loaded from cache") {
                    shouldHaveTask(":dokkaGeneratePublicationHtml").shouldHaveOutcome(FROM_CACHE)
                }
                withClue("only one project is documented, so expect no Dokka module generation") {
                    shouldNotHaveRunTask(":dokkaGenerateModuleHtml")
                }
            }
    }

    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-jvm")
    fun `expect Dokka is compatible with Gradle Configuration Cache`(
        project: DokkaGradleProjectRunner,
        testedVersions: TestedVersions,
    ) {
        val configCacheRunner =
            project.runner.addArguments(
                "clean",
                ":dokkaGenerate",
                "--no-build-cache",
                "--configuration-cache",
            )

        withClue("first build should store the configuration cache") {
            project.deleteConfigurationCacheData()
            configCacheRunner.build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)

                output shouldContain "Configuration cache entry stored"

                testConfigurationCacheResult(project, testedVersions)
            }
        }

        withClue("second build should reuse the configuration cache") {
            configCacheRunner.build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)
                output shouldContain "Configuration cache entry reused"
            }
        }
    }

    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-jvm")
    fun `expect Gradle Configuration Cache can be re-used`(
        project: DokkaGradleProjectRunner,
        testedVersions: TestedVersions,
    ) {
        assumeTrue(testedVersions.hasGradleVersionThatSupportsCcReuse()) {
            "CC re-use is only supported in Gradle 9.1 or 9.4+ (it's bugged in 9.2 and 9.3)"
        }

        val configCacheRunner =
            project.runner.addArguments(
                "clean",
                ":dokkaGenerate",
                "--configuration-cache",
            )

        withClue("first build should store the configuration cache") {
            project.deleteConfigurationCacheData()
            configCacheRunner.build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)

                output shouldContain "Configuration cache entry stored"

                testConfigurationCacheResult(project, testedVersions)
            }
        }

        withClue("unrelated properties should not cause CC misses") {
            configCacheRunner.addArguments(
                "-PunusedProperty=unusedValue",
            ).build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)
                output shouldContain "Configuration cache entry reused"
            }
        }
    }
}
