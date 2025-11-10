/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.it.gradle.junit.TestedVersionsSource.Default.dokkaVersionOverride
import org.jetbrains.dokka.it.gradle.utils.SemVer
import org.jetbrains.dokka.it.gradle.utils.SemVer.Companion.contains
import org.jetbrains.dokka.it.gradle.utils.SemVerRange
import org.jetbrains.dokka.it.optionalSystemProperty
import org.jetbrains.dokka.it.systemProperty

/**
 * Provide a sequence of [TestedVersions] to re-run tests with different combinations of versions.
 */
fun interface TestedVersionsSource<T : TestedVersions> {

    fun get(): Sequence<T>

    /**
     * Provides [TestedVersions.Default] for a default DGP integration test.
     */
    object Default : TestedVersionsSource<TestedVersions.Default> {

        private val dokkaVersionOverride: String? by optionalSystemProperty()
        private val dokkaVersion: String by systemProperty { dokkaVersionOverride ?: it }

        /**
         * Dokka Gradle Plugin versions to test.
         *
         * This can be overridden via system property [dokkaVersionOverride].
         */
        private val allDokkaGradlePluginVersions: List<String> = listOf(
            dokkaVersion
        )

        /**
         * Kotlin Gradle Plugin versions that DGP will be tested.
         *
         * We aim to support the current and previous two releases.
         *
         * Currently we don't test Kotlin 1.8 because DGPv2 is experimental,
         * with a planned stable release with Kotlin 2.1.
         * When DGPv2 is released as stable, then we will support the last three Kotlin releases.
         */
        private val allKgpVersions: List<String> = listOf(
            "1.9.25",
            "2.0.21",
            "2.1.21",
            "2.2.21",
            "2.3.0-Beta2",
        )

        /**
         * Gradle versions to test.
         *
         * We test the latest v7 and v8 Gradle versions supported by KGP and Android.
         */
        private val allGradleVersions: List<String> = listOf(
            "7.6.4",
            "8.14.3",
            "9.1.0",
        )

        private val allVersions: Sequence<TestedVersions.Default> =
            sequence {
                allDokkaGradlePluginVersions.forEach { dgp ->
                    allKgpVersions.forEach { kgp ->
                        allGradleVersions.forEach { gradle ->
                            yield(
                                TestedVersions.Default(
                                    dgp = SemVer(dgp),
                                    gradle = SemVer(gradle),
                                    kgp = SemVer(kgp),
                                )
                            )
                        }
                    }
                }
            }.filter { isKgpCompatibleWithGradle(kgp = it.kgp, gradle = it.gradle) }

        override fun get(): Sequence<TestedVersions.Default> = allVersions

        private fun isKgpCompatibleWithGradle(kgp: SemVer, gradle: SemVer): Boolean {
            return when {
                kgp >= SemVer("2.1.20") -> gradle.major >= 9
                else -> gradle.major < 9
            }
        }
    }

    /**
     * Provide [TestedVersions.Android] versions to run Android projects.
     *
     * The test must be tagged with [TestsAndroid].
     */
    class Android(
        private val agpVersionRange: SemVerRange? = null,
    ) : TestedVersionsSource<TestedVersions.Android> {

        /**
         * All possible Android Gradle Plugin versions that could be tested.
         *
         * We test the latest v7, v8, and v9 AGP versions.
         *
         * Note the AGP major version indicates the required major version of Gradle.
         * So, AGP 7.* supports Gradle 7, but will throw an error if used with Gradle 8.
         */
        private val allAgpVersions: List<SemVer> = listOf(
            "7.4.2",
            "8.11.1",
            "8.12.0",
            "9.0.0-alpha13",
        ).map { SemVer(it) }

        private val matchedAgpVersions: List<SemVer> =
            if (agpVersionRange == null) {
                allAgpVersions
            } else {
                allAgpVersions.filter { it in agpVersionRange }
            }

        init {
            require(matchedAgpVersions.isNotEmpty()) {
                "No AGP versions matched the given range: $agpVersionRange"
            }
        }

        private val allVersions: Sequence<TestedVersions.Android> =
            sequence {
                Default.get().forEach { v ->
                    matchedAgpVersions.forEach { agp ->
                        yield(
                            TestedVersions.Android(
                                dgp = v.dgp,
                                gradle = v.gradle,
                                kgp = v.kgp,
                                agp = agp,
                            )
                        )
                    }
                }
            }.filter { v ->
                isAgpCompatibleWithGradle(agp = v.agp, gradle = v.gradle)
            }

        override fun get(): Sequence<TestedVersions.Android> = allVersions

        private fun isAgpCompatibleWithGradle(agp: SemVer, gradle: SemVer): Boolean {
            // AGP/Gradle compatibility definitions:
            // https://developer.android.com/build/releases/gradle-plugin?buildsystem=ndk-build#updating-gradle
            return when (agp.majorAndMinorVersions) {
                "9.0" -> gradle.major >= 9
                "8.12" -> gradle in "8.13.0".."9.0.0"
                "8.11" -> gradle in "8.13.0".."9.0.0"
                "8.10" -> gradle in "8.11.1"..<"9.0.0"
                "8.9" -> gradle in "8.11.1"..<"9.0.0"
                "8.8" -> gradle in "8.10.2"..<"9.0.0"
                "8.7" -> gradle in "8.9.0"..<"9.0.0"
                "8.6" -> gradle in "8.7.0"..<"9.0.0"
                "8.5" -> gradle in "8.7.0"..<"9.0.0"
                "8.4" -> gradle in "8.6.0"..<"9.0.0"
                "8.3" -> gradle in "8.4.0"..<"9.0.0"
                "8.2" -> gradle in "8.2.0"..<"9.0.0"
                "8.1" -> gradle in "8.0.0"..<"9.0.0"
                "8.0" -> gradle in "8.0.0"..<"9.0.0"
                "7.4" -> gradle in "7.5.0"..<"8.0.0"
                else -> true
            }
        }
    }


    /**
     * Provide [TestedVersions.AndroidCompose] versions to run Android Compose projects.
     *
     * The test must be tagged with [TestsAndroidCompose].
     */
    class AndroidCompose(
        agpVersionRange: SemVerRange? = null,
    ) : TestedVersionsSource<TestedVersions.AndroidCompose> {

        private val androidVersions = Android(agpVersionRange)

        /**
         * Versions of the `org.jetbrains.compose` plugins.
         */
        private val allComposeGradlePluginVersions: List<String> = listOf(
            // https://github.com/JetBrains/compose-multiplatform/blob/c52b4a92b94714424dfdd73f6f91a55fec359a69/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/ComposeCompilerCompatibility.kt#L6
            "1.7.0",
        )

        override fun get(): Sequence<TestedVersions.AndroidCompose> = sequence {
            androidVersions.get().forEach { v ->
                allComposeGradlePluginVersions.forEach { composeGP ->
                    yield(
                        TestedVersions.AndroidCompose(
                            dgp = v.dgp,
                            gradle = v.gradle,
                            kgp = v.kgp,
                            agp = v.agp,
                            composeGradlePlugin = SemVer(composeGP),
                        )
                    )
                }
            }
        }
    }
}
