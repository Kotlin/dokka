/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnectionException
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.jetbrains.dokka.it.systemProperty
import org.jetbrains.dokka.it.withJvmArguments
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.test.BeforeTest

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

        /** file-based Maven repositories that contains the Dokka dependencies */
        val projectLocalMavenDirs: List<Path> by systemProperty { it.split(":").map(Paths::get) }

        fun File.updateProjectLocalMavenDir() {

            val repositories =
                projectLocalMavenDirs.joinToString(", ") { "maven(\"${it.invariantSeparatorsPathString}\")" }

            walk().filter { it.isFile }.forEach { file ->
                file.writeText(
                    file.readText()
                        .replace(
                            "/* %{PROJECT_LOCAL_MAVEN_DIR}% */",
                            /*language=TEXT*/
                            """
                                |exclusiveContent {
                                |    forRepositories($repositories)
                                |    filter {
                                |        includeGroup("org.jetbrains.dokka")
                                |    }
                                |}
                                |
                            """.trimMargin()
                        )
                )
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
