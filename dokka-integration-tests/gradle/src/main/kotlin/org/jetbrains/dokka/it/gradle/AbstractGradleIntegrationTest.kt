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
import kotlin.test.BeforeTest

abstract class AbstractGradleIntegrationTest : AbstractIntegrationTest() {

    @BeforeTest
    fun copyTemplates() {
        File("projects").listFiles().orEmpty()
            .filter { it.isFile }
            .filter { it.name.startsWith("template.") }
            .forEach { file -> file.copyTo(File(tempFolder, file.name)) }
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
        val exampleProjectsDir: Path by systemProperty(Paths::get)
        val templateProjectsDir: Path by systemProperty(Paths::get)

        /** file-based Maven repositories that contains the Dokka dependencies */
        val projectLocalMavenDirs: List<Path> by systemProperty { it.split(":").map(Paths::get) }

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
