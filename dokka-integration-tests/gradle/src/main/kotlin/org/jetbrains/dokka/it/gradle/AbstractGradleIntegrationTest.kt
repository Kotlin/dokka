/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnectionException
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.jetbrains.dokka.it.systemProperty
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.test.BeforeTest
import kotlin.time.Duration.Companion.seconds

abstract class AbstractGradleIntegrationTest : AbstractIntegrationTest() {

    @BeforeTest
    open fun beforeEachTest() {
        prepareProjectFiles()
    }

    fun prepareProjectFiles(
        templateProjectDir: Path = AbstractGradleIntegrationTest.templateProjectDir,
        destination: File = projectDir,
    ) {
        templateProjectDir.copyToRecursively(destination.toPath(), followLinks = false, overwrite = true)
        templateSettingsGradleKts.copyTo(destination.resolve("template.settings.gradle.kts").toPath(), overwrite = true)
        destination.updateProjectLocalMavenDir()
    }

    fun createGradleRunner(
        buildVersions: BuildVersions,
        vararg arguments: String,
        jvmArgs: List<String> = listOf("-Xmx2G", "-XX:MaxMetaspaceSize=1G")
    ): GradleRunner {

        // TODO quick hack to add `android { namespace }` on AGP 7+ (it's mandatory in 8+).
        //      This hack could be made prettier, or only test AGP 7+
        val androidMajorVersion = buildVersions.androidGradlePluginVersion
            ?.substringBefore(".")
            ?.toIntOrNull() ?: 0
        if (androidMajorVersion >= 7) {
            projectDir.resolve("build.gradle.kts").appendText(
                """
                |
                |android {
                |    namespace = "org.jetbrains.dokka.it.android"
                |}
                |
            """.trimMargin()
            )
        }

        return GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withJetBrainsCachedGradleVersion(buildVersions.gradleVersion)
            .withTestKitDir(File("build", "gradle-test-kit").absoluteFile)
            .withDebug(TestEnvironment.isEnabledDebug)
            .withArguments(
                listOfNotNull(
                    "-Pdokka_it_dokka_version=${System.getenv("DOKKA_VERSION")}",
                    "-Pdokka_it_kotlin_version=${buildVersions.kotlinVersion}",
                    buildVersions.androidGradlePluginVersion?.let { androidVersion ->
                        "-Pdokka_it_android_gradle_plugin_version=$androidVersion"
                    },
                    // property flag to use K2
                    if (TestEnvironment.shouldUseK2())
                        "-P${TestEnvironment.TRY_K2}=true"
                    else
                        null,

                    // Decrease Gradle daemon idle timeout to prevent old agents lingering on CI.
                    // A lower timeout means slower tests, which is preferred over OOMs and locked processes.
                    "-Dorg.gradle.daemon.idletimeout=" + 10.seconds.inWholeMilliseconds, // default is 3 hours!
                    "-Pkotlin.daemon.options.autoshutdownIdleSeconds=10",

                    * arguments
                )
            ).withJvmArguments(jvmArgs)
    }

    fun GradleRunner.buildRelaxed(): BuildResult {
        return try {
            build()
        } catch (e: Throwable) {
            val gradleConnectionException = e.withAllCauses().find { it is GradleConnectionException }
            if (gradleConnectionException != null) {
                gradleConnectionException.printStackTrace()
                throw IllegalStateException("Assumed Gradle connection", gradleConnectionException)

            }
            throw e
        }
    }

    companion object {
        /**
         * Location of the template project that will be copied into [AbstractIntegrationTest.projectDir].
         *
         * The contents of this directory _must not_ be modified.
         *
         * The value is provided by the Gradle Test task.
         */
        val templateProjectDir: Path by systemProperty(Paths::get)

        /**
         * Location of the `template.settings.gradle.kts` file used to provide common Gradle Settings configuration for template projects.
         *
         * This value is provided by the Gradle Test task.
         */
        val templateSettingsGradleKts: Path by systemProperty(Paths::get)

        /** file-based Maven repositories with Dokka dependencies */
        private val devMavenRepositories: String by systemProperty { repos ->
            val repoPaths = repos.split(",").map { Paths.get(it) }

            val reposSpecs = repoPaths
                .withIndex()
                .joinToString(",\n") { (i, repoPath) ->
                    // Exclusive repository containing local Dokka artifacts.
                    // Must be compatible with both Groovy and Kotlin DSL.
                    /* language=kts */
                    """
                    |maven {
                    |    setUrl("${repoPath.invariantSeparatorsPathString}")
                    |    name = "DokkaDevMavenRepo${i}"
                    |}
                    """.trimMargin()
                }

            /* language=kts */
            """
            |exclusiveContent {
            |    forRepositories(
            |      $reposSpecs
            |    )
            |    filter {
            |        includeGroup("org.jetbrains.dokka")
            |    }
            |}
            |
            """.trimMargin()
        }

        fun File.updateProjectLocalMavenDir() {

            val dokkaDevMavenRepoMarker = "/* %{PROJECT_LOCAL_MAVEN_DIR}% */"

            // Exclusive repository containing local Dokka artifacts.
            // Must be compatible with both Groovy and Kotlin DSL.

            walk().filter { it.isFile }.forEach { file ->
                val fileText = file.readText()

                if (dokkaDevMavenRepoMarker in fileText) {
                    file.writeText(
                        fileText.replace(dokkaDevMavenRepoMarker, devMavenRepositories)
                    )
                }
            }
        }
    }
}

private fun GradleRunner.withJetBrainsCachedGradleVersion(version: GradleVersion): GradleRunner {
    return withGradleDistribution(
        URI.create(
            "https://cache-redirector.jetbrains.com/" +
                    "services.gradle.org/distributions/" +
                    "gradle-${version.version}-bin.zip"
        )
    )
}

private fun Throwable.withAllCauses(): Sequence<Throwable> {
    val root = this
    return sequence {
        yield(root)
        val cause = root.cause
        if (cause != null && cause != root) {
            yieldAll(cause.withAllCauses())
        }
    }
}
