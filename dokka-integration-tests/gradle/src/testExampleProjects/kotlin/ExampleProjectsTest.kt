/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.examples

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.it.gradle.TestConstants
import org.jetbrains.dokka.it.gradle.assertNoUnknownClassErrorsInHtml
import org.jetbrains.dokka.it.gradle.examples.ExampleProjectsTest.TestCase.Companion.exampleProjectFilter
import org.jetbrains.dokka.it.gradle.examples.ExampleProjectsTest.TestCaseProvider.Companion.dokkaVersion
import org.jetbrains.dokka.it.gradle.loadConfigurationCacheReportData
import org.jetbrains.dokka.it.gradle.shouldHaveOutcome
import org.jetbrains.dokka.it.gradle.shouldHaveTask
import org.jetbrains.dokka.it.optionalSystemProperty
import org.jetbrains.dokka.it.systemProperty
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.*
import kotlin.streams.asStream

/**
 * Test all Dokka Gradle v2 examples.
 *
 * Each test is parameterised and will run for each example.
 *
 * #### Testing a specific project.
 *
 * To run a `@Test` for a single project the `build.gradle.kts` defines a test task for each example project.
 * So, to only test the `basic-gradle-example`, run `gradle :dokka-integration-tests:gradle:testBasicGradleExample`.
 * All other projects will be skipped.
 */
class ExampleProjectsTest {

    class TestCaseProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
            val exampleProjectDirs = exampleGradleProjectsDir
                .listDirectoryEntries()
                .filter { it.isDirectory() }

            val temporaryDir = createTempDirectory()

            return exampleProjectDirs
                .asSequence()
                .map { exampleProjectDir ->
                    val projectDestinationDir = temporaryDir.resolve(exampleProjectDir.name)

                    val exampleProject = initProject(exampleProjectDir, projectDestinationDir)
                    val expectedDataDir = expectedDataDir.resolve("exampleProjects").resolve(exampleProjectDir.name)

                    named(
                        exampleProjectDir.name,
                        TestCase(
                            project = exampleProject,
                            expectedDataDir = expectedDataDir,
                        )
                    )
                }
                .map { arguments(it) }
                .asStream()
        }

        companion object {
            /** Base directory that contains all Dokka Gradle example projects. */
            private val exampleGradleProjectsDir by systemProperty(::Path)

            /** Base directory that contains all expected output data for the Gradle example projects. */
            private val expectedDataDir by systemProperty(::Path)

            private val dokkaVersionOverride: String? by optionalSystemProperty()
            private val dokkaVersion: String by systemProperty { dokkaVersionOverride ?: it }

            /** Create a new [GradleProjectTest] copied from the source project in [sourceProjectDir]. */
            private fun initProject(
                sourceProjectDir: Path,
                destinationDir: Path,
            ): GradleProjectTest {
                return GradleProjectTest(destinationDir).apply {
                    sourceProjectDir.copyToRecursively(projectDir, overwrite = true, followLinks = false)
                    updateSettingsRepositories()
                    updateDokkaVersion()
                }
            }

            private fun GradleProjectTest.updateDokkaVersion() {
                projectDir.walk()
                    .filter { it.name == "build.gradle.kts" }
                    .forEach { buildFile ->
                        buildFile.writeText(
                            buildFile.readText()
                                .replaceDokkaPluginsVersion()
                                .replaceDokkaDependencyCoords()
                        )
                    }
            }

            /** Replace the version of any Dokka plugin with [dokkaVersion]. */
            private fun String.replaceDokkaPluginsVersion(): String =
                replace(
                    """(id\("org\.jetbrains\.dokka[^"]*"\) version ")[^"]+(")""".toRegex(),
                    """$1${dokkaVersion}$2""",
                )

            /** Replace the version of any Dokka dependency coord with [dokkaVersion]. */
            private fun String.replaceDokkaDependencyCoords(): String =
                replace(
                    """("org\.jetbrains\.dokka:[^:]*:)[^"]+(")""".toRegex(),
                    """$1${dokkaVersion}$2""",
                )
        }
    }

    data class TestCase(
        val project: GradleProjectTest,
        val expectedDataDir: Path,
    ) {
        val exampleProjectName = ExampleProject.of(project.projectDir)

        /**
         * Tests are enabled if there is no [exampleProjectFilter] set, or if [exampleProjectFilter] matches
         * the name of [GradleProjectTest.projectDir].
         */
        val isEnabled: Boolean =
            exampleProjectFilter == null || project.projectDir.name == exampleProjectFilter

        /** `true` if the project produces Dokka HTML. */
        val outputsHtml: Boolean =
            when (exampleProjectName) {
                ExampleProject.Javadoc -> false
                else -> true
            }

        /** `true` if the project produces Dokka Javadoc. */
        val outputsJavadoc: Boolean =
            when (exampleProjectName) {
                ExampleProject.Javadoc -> true
                ExampleProject.LibraryPublishing -> true
                else -> false
            }

        /** Base directory for the  */
        private val dokkaOutputPath: String =
            when (exampleProjectName) {
                ExampleProject.VersioningMultimodule -> "docs/build/dokka/"
                ExampleProject.Multimodule -> "docs/build/dokka/"
                ExampleProject.CompositeBuild -> "docs/build/dokka/"
                ExampleProject.CustomDokkaPlugin -> "demo-library/build/dokka/"
                else -> "build/dokka/"
            }

        val dokkaOutputDir: Path = project.projectDir.resolve(dokkaOutputPath)

        val dokkaGenerateTask: String = when (exampleProjectName) {
            ExampleProject.VersioningMultimodule -> ":docs:dokkaGenerate"
            ExampleProject.Multimodule -> ":docs:dokkaGenerate"
            ExampleProject.CompositeBuild -> ":build"
            ExampleProject.CustomDokkaPlugin -> ":demo-library:dokkaGenerate"
            else -> ":dokkaGenerate"
        }

        init {
            updateGradleProperties()
        }

        private fun updateGradleProperties() {
            when (exampleProjectName) {
                ExampleProject.KotlinMultiplatform -> {
                    project.gradleProperties {
                        // kotlin.native.enableKlibsCrossCompilation must be set to `true`
                        // otherwise Kotlin can't generate a Klib for Coroutines in macosMain
                        // when generating on Linux machines, resulting in 'Error class: unknown class'
                        // for CoroutineScope appearing in the generated docs.
                        kotlin.native.enableKlibsCrossCompilation = true
                    }
                }

                else -> {}
            }

            project.runner.writeGradleProperties(project.gradleProperties)
        }

        companion object {
            /**
             * If set, only run a specific example project with the matching [exampleProjectName].
             *
             * This property is set in the Gradle build config.
             */
            private val exampleProjectFilter by optionalSystemProperty()
        }
    }

    /**
     * Identifier for each example project, to avoid string comparisons everywhere.
     */
    enum class ExampleProject {
        BasicGradle,
        CompositeBuild,
        CustomDokkaPlugin,
        CustomStyling,
        Java,
        Javadoc,
        KotlinAsJava,
        KotlinMultiplatform,
        LibraryPublishing,
        Multimodule,
        VersioningMultimodule,
        ;

        companion object {
            fun of(dir: Path): ExampleProject {
                return when (dir.name) {
                    "basic-gradle-example" -> BasicGradle
                    "javadoc-example" -> Javadoc
                    "composite-build-example" -> CompositeBuild
                    "custom-dokka-plugin-example" -> CustomDokkaPlugin
                    "custom-styling-example" -> CustomStyling
                    "java-example" -> Java
                    "kotlin-as-java-example" -> KotlinAsJava
                    "kotlin-multiplatform-example" -> KotlinMultiplatform
                    "library-publishing-example" -> LibraryPublishing
                    "multimodule-example" -> Multimodule
                    "versioning-multimodule-example" -> VersioningMultimodule
                    else -> error("Undeclared example project: $dir")
                }
            }
        }
    }


    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test HTML output`(testCase: TestCase) {
        assumeTrue(testCase.isEnabled)
        assumeTrue(testCase.outputsHtml)

        testDokkaOutput(
            testCase = testCase,
            format = "html",
            filesExcludedFromContentCheck = TestConstants.DokkaHtmlAssetsFiles,
        )

        assertNoUnknownClassErrorsInHtml(
            dokkaOutputDir = testCase.dokkaOutputDir.resolve("html")
        )
    }

    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test Javadoc output`(testCase: TestCase) {
        assumeTrue(testCase.isEnabled)
        assumeTrue(testCase.outputsJavadoc)

        testDokkaOutput(
            testCase = testCase,
            format = "javadoc",
        )
    }

    private fun testDokkaOutput(
        testCase: TestCase,
        format: String,
        filesExcludedFromContentCheck: List<String> = emptyList(),
    ) {
        val expectedDataDir = testCase.expectedDataDir.resolve(format)
        val actualHtmlDir = testCase.dokkaOutputDir.resolve(format)

        assert(expectedDataDir.isDirectory()) {
            "Missing expectedDataDir: ${expectedDataDir.toUri()}"
        }

        testCase.project.runner
            .addArguments(
                testCase.dokkaGenerateTask,
                "--stacktrace",
            )
            .build {
                actualHtmlDir.shouldBeADirectory()

                withClue(
                    """
                    |expectedDataDir: ${expectedDataDir.toUri()}
                    |actualHtmlDir: ${actualHtmlDir.toUri()}
                    """.trimMargin()
                ) {
                    withClue("expect file trees are the same") {
                        val expectedFileTree = expectedDataDir.toTreeString()
                        val actualFileTree = actualHtmlDir.toTreeString()
                        actualFileTree shouldBe expectedFileTree
                    }

                    withClue("expect directories are the same") {
                        actualHtmlDir.shouldBeADirectoryWithSameContentAs(
                            expectedDataDir,
                            filesExcludedFromContentCheck
                        )
                    }
                }
            }
    }

    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test build cache`(
        testCase: TestCase,
        @TempDir tempDir: Path,
    ) {
        assumeTrue(testCase.isEnabled)

        val buildCacheDir = tempDir.resolve("build-cache").apply {
            deleteRecursively()
            createDirectories()
        }

        testCase.project.settingsGradleKts += """
            |buildCache {
            |    local {
            |        directory = File("${buildCacheDir.invariantSeparatorsPathString}")
            |    }
            |}
            """.trimMargin()

        // Initial build, to populate the build cache.
        testCase.project.runner
            .addArguments(
                testCase.dokkaGenerateTask,
                "--stacktrace",
            )
            .build {
                output shouldContain "BUILD SUCCESSFUL"
            }

        // Clean local directories, to ensure tasks are loaded from build cache.
        testCase.project.runner
            .addArguments(
                "clean",
                "--stacktrace",
            )
            .build {
                output shouldContain "BUILD SUCCESSFUL"
            }

        // Re-run generate, and verify that the Dokka tasks are cached.
        testCase.project.runner
            .addArguments(
                testCase.dokkaGenerateTask,
                "--stacktrace",
                "--build-cache",
            )
            .build {
                when (testCase.exampleProjectName) {

                    ExampleProject.Javadoc -> {
                        shouldHaveTasksWithOutcome(
                            ":dokkaGeneratePublicationJavadoc" to FROM_CACHE,
                            ":dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    ExampleProject.CompositeBuild -> {
                        shouldHaveTasksWithOutcome(
                            ":module-kakapo:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":module-kea:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":docs:dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":docs:dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    ExampleProject.CustomDokkaPlugin -> {
                        shouldHaveTasksWithOutcome(
                            ":demo-library:dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":demo-library:dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    ExampleProject.Multimodule -> {
                        shouldHaveTasksWithOutcome(
                            ":childProjectA:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":childProjectB:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":docs:dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":docs:dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    ExampleProject.VersioningMultimodule -> {
                        shouldHaveTasksWithOutcome(
                            ":childProjectA:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":childProjectB:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":docs:dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":docs:dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    else -> {
                        shouldHaveTasksWithOutcome(
                            ":dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":dokkaGenerate" to UP_TO_DATE,
                        )
                    }
                }
            }
    }


    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test configuration cache`(testCase: TestCase) {
        assumeTrue(testCase.isEnabled)

        // delete old configuration cache results and reports, to make sure we can fetch the newest report
        val ccReportFileMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:**/build/reports/configuration-cache/**/configuration-cache-report.html")
        testCase.project
            .findFiles { file -> ccReportFileMatcher.matches(file) }
            .distinct()
            .forEach { it.deleteExisting() }

        val configCacheRunner: GradleRunner =
            testCase.project.runner
                .addArguments(
                    testCase.dokkaGenerateTask,
                    "--stacktrace",
                    "--configuration-cache",
                )

        // first build should store the configuration cache
        configCacheRunner.build {
            shouldHaveTask(testCase.dokkaGenerateTask).shouldHaveOutcome(UP_TO_DATE, SUCCESS)

            output shouldContain "Configuration cache entry stored"

            loadConfigurationCacheReportData(projectDir = testCase.project.projectDir)
                .asClue { ccReport ->
                    ccReport.totalProblemCount shouldBe 0
                }
        }

        withClue("KT-66423 KGP needs another build to finish setting up kotlin-native") {
            configCacheRunner.build {
                shouldHaveTask(testCase.dokkaGenerateTask).shouldHaveOutcome(UP_TO_DATE, SUCCESS)
            }
        }

        // second build should reuse the configuration cache
        configCacheRunner.build {
            shouldHaveTask(testCase.dokkaGenerateTask).shouldHaveOutcome(UP_TO_DATE, SUCCESS)
            output shouldContain "Configuration cache entry reused"
        }
    }
}
