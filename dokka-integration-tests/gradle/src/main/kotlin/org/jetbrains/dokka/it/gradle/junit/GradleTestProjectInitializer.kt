/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest.Companion.updateProjectLocalMavenDir
import org.jetbrains.dokka.it.gradle.junit.DokkaGradlePluginTestExtension.Companion.getAndroidSdkDir
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Initialise a Gradle Test project into a directory.
 *
 * The source directory must not be modified.
 */
fun interface GradleTestProjectInitializer {
    fun initialize(
        source: Path,
        destination: Path,
        testedVersions: TestedVersions,
        gradleProperties: Map<String, String>
    )

    object Default : GradleTestProjectInitializer {
        override fun initialize(
            source: Path,
            destination: Path,
            testedVersions: TestedVersions,
            gradleProperties: Map<String, String>,
        ) {
            println("Source: $source")
            require(source.isDirectory()) { "Source project must be a directory. Actual: $source" }
            source.copyToRecursively(destination, overwrite = true, followLinks = false)

            destination.updateProjectLocalMavenDir()

            val gradlePropertiesFile = destination.resolve("gradle.properties").apply {
                deleteIfExists()
                createFile()
            }

            gradlePropertiesFile.writeText(
                gradleProperties.entries
                    .map { (k, v) -> "$k=$v" }
                    .sorted()
                    .joinToString(separator = "\n", postfix = "\n")
            )

            destination.walk().filter { it.isRegularFile() }.forEach { file ->
                file.writeText(
                    file.readText()
                        .replace("/* %{KGP_VERSION} */", testedVersions.kgp.version)
                        .replace("/* %{DGP_VERSION} */", testedVersions.dgp.version)
                        .run {
                            if (testedVersions.agp != null) {
                                replace("/* %{AGP_VERSION} */", testedVersions.agp.version)
                            } else {
                                this
                            }
                        }
                        .run {
                            if (testedVersions.composeGradlePlugin != null) {
                                replace("/* %{COMPOSE_VERSION} */", testedVersions.composeGradlePlugin.version)
                            } else {
                                this
                            }
                        }
                )
            }

            if (testedVersions.agp != null) {
                initialiseAndroid(
                    destination = destination
                )
            }
        }

        private fun initialiseAndroid(
            destination: Path,
        ) {
            destination.resolve("local.properties").apply {
                writeText(
                    """
                    |sdk.dir=${getAndroidSdkDir()}
                    |
                    """.trimMargin()
                )
            }
        }
    }
}
