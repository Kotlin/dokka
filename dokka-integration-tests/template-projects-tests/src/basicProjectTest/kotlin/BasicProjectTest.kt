/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.basic_project

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.it.GradleProjectTest
import org.jetbrains.dokka.it.GradleProjectTest.Companion.projectLocalMavenDirs
import org.jetbrains.dokka.it.GradleProjectTest.Companion.templateProjectsDir
import org.jetbrains.dokka.it.build
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.streams.asStream
import kotlin.test.assertContains
import kotlin.test.assertEquals


class BasicProjectTest {

    @ParameterizedTest
    @ArgumentsSource(TestParamsProvider::class)
    fun help(
        testParams: TestParams,
        @TempDir(cleanup = ON_SUCCESS) tempDirectory: File,
    ) {
        val (kgpVersion, gradleVersion, jvmVersion) = testParams

        assumeTrue(
            jvmVersion == "8",
            "TODO skip other JVM versions, since the JVM version can't be changed at the moment",
        )
        val tmpDir =
            tempDirectory.resolve(
                "${kgpVersion}_${gradleVersion}_${jvmVersion}".map { if (it.isLetterOrDigit()) it else "_" }
                    .joinToString("")
            )

        val basicProjectDir = templateProjectsDir.resolve("basicProject").toFile()
        basicProjectDir.copyRecursively(tmpDir)
        tmpDir.walk().filter { it.isFile }.forEach { file ->
            file.writeText(
                file.readText()
                    .replace(
                        "/* %{PROJECT_LOCAL_MAVEN_DIR}% */",
                        projectLocalMavenDirs.joinToString("\n") {
                            """maven("${it.invariantSeparatorsPathString}")"""
                        }
                    )
                    .replace("%{KOTLIN_VERSION}%", kgpVersion)
                    .replace("%{DOKKA_VERSION}%", "1.9.20-SNAPSHOT")
            )
        }

        val project = GradleProjectTest(tmpDir.toPath())

        project.runner
            .withGradleVersion(gradleVersion)
            .withArguments("help")
            .build {
                assertAll(tasks.map { { assertEquals(SUCCESS, it.outcome) } })
                assertContains(output, "BUILD SUCCESSFUL")
            }
    }

//    @TestFactory
//    fun dynamicTestsWithCollection(
//        @TempDir(cleanup = NEVER) tempDirectory: File
//    ): Collection<DynamicNode> {
//
//        val kgpVersions = listOf("1.8.20", "1.9.21")
//        val gradleVersions = listOf("7.6.3", "8.5")
//        val jvmVersions = listOf("8", "11", "17", "21")
//
//        return jvmVersions.map { jvmVersion ->
//            dynamicContainer("jvm:$jvmVersion", gradleVersions.map { gradleVersion ->
//                dynamicContainer("gradle:$gradleVersion", kgpVersions.map { kgpVersion ->
//                    dynamicContainer(
//                        "kgp:$kgpVersion", listOf(
//                            dynamicTest("run 'help'") {
//                                assumeTrue(
//                                    jvmVersion == "8",
//                                    "TODO skip other JVM versions, since the JVM version can't be changed at the moment",
//                                )
//                                val tmpDir =
//                                    tempDirectory.resolve(
//                                        "${kgpVersion}_${gradleVersion}_${jvmVersion}".map { if (it.isLetterOrDigit()) it else "_" }
//                                            .joinToString("")
//                                    )
//
//                                val basicProjectDir = templateProjectsDir.resolve("basicProject").toFile()
//                                basicProjectDir.copyRecursively(tmpDir)
//                                tmpDir.walk().filter { it.isFile }.forEach {
//                                    it.writeText(
//                                        it.readText()
//                                            .replace(
//                                                "%{PROJECT_LOCAL_MAVEN_DIR}%",
//                                                projectLocalMavenDir.invariantSeparatorsPathString
//                                            )
//                                            .replace("%{KOTLIN_VERSION}%", kgpVersion)
//                                            .replace("%{DOKKA_VERSION}%", "1.9.20-SNAPSHOT")
//                                    )
//                                }
//
//                                val project = GradleProjectTest(tmpDir.toPath())
//
//                                project.runner
//                                    .withGradleVersion(gradleVersion)
//                                    .withArguments("help")
//                                    .build {
//                                        assertAll(tasks.map { { assertEquals(SUCCESS, it.outcome) } })
//                                        assertContains(output, "BUILD SUCCESSFUL")
//                                    }
//                            })
//                    )
//                })
//            })
//        }
//    }
}


data class TestParams(
    val kgpVersion: String,
    val gradleVersion: String,
    val jvmVersion: String,
)

private object TestParamsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {

        val kgpVersions = listOf("1.8.20", "1.9.21")
        val gradleVersions = listOf("7.6.3", "8.5")
        val jvmVersions = listOf("8", "11", "17", "21")

        return sequence {
            kgpVersions.forEach { kgpVersion ->
                gradleVersions.forEach { gradleVersion ->
                    jvmVersions.forEach { jvmVersion ->
                        yield(TestParams(kgpVersion, gradleVersion, jvmVersion))
                    }
                }
            }
        }.map { Arguments.of(it) }.asStream()
    }

}
