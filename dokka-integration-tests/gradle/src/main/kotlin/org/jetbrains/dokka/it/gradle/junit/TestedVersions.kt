/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.it.gradle.utils.SemVer
import org.jetbrains.dokka.it.optionalSystemProperty
import org.jetbrains.dokka.it.systemProperty


/**
 * The parameters used to run each invocation of a [DokkaGradlePluginTest] test.
 *
 * [DokkaGradlePluginTestExtension] generates all possible values, and creates a test invocation
 * for each valid combination.
 *
 * To access the specific values used for a test invocation, add a [TestedVersions]
 * parameter in the test function.
 */
data class TestedVersions(
    /** Kotlin Gradle Plugin version. */
    val kgp: SemVer,
    /** Gradle version. */
    val gradle: SemVer,
    /** Dokka Gradle Plugin version. */
    val dgp: SemVer,
    /**
     * The Android Gradle Plugin version being tested.
     *
     * The test must be annotated with [TestsAndroidGradlePlugin] for this value to be set.
     */
    val agp: SemVer?,
    /**
     * The Compose version being tested.
     *
     * The test must be annotated with [TestsCompose] for this value to be set.
     */
    val composeGradlePlugin: SemVer?,
) : Comparable<TestedVersions> {

    constructor(
        kgp: String,
        gradle: String,
        dgp: String,
        agp: String?,
        composeGradlePlugin: String?,
    ) : this(
        kgp = SemVer(kgp),
        gradle = SemVer(gradle),
        dgp = SemVer(dgp),
        agp = agp?.let { SemVer(it) },
        composeGradlePlugin = composeGradlePlugin?.let { SemVer(it) },
    )

    /**
     * Check if the combination of versions is valid.
     *
     * For example, AGP 7 must use Gradle 7. If [agp] is `8.0.0` and [gradle] is `7.0.0`
     * then the combination is invalid.
     */
    private fun isValid(): Boolean {
        if (agp != null) {
            if (agp.major != gradle.major) {
                return false
            }

            when (kgp) {
                SemVer("1.9.0") -> {
                    if (agp !in SemVer("7.0.0")..SemVer("8.2.0")) {
                        return false
                    }
                }
            }
        }

        return true
    }

    private val versionsMap: Map<String, SemVer> =
        buildMap {
            put("Dokka", dgp)
            put("Kotlin", kgp)
            put("Gradle", gradle)
            if (agp != null) put("Android", agp)
            if (composeGradlePlugin != null) put("Compose", composeGradlePlugin)
        }

    /**
     * Join the major and minor versions of each version.
     * Used by [allTestedVersions] to filter out duplicated patch versions.
     */
    private val majorAndMinorVersions: String =
        versionsMap
            .mapValues { (_, v) -> "${v.major}.${v.minor}" }
            .entries
            .map { (k, v) -> "$k: $v" }
            .sorted().joinToString(" ")

    /**
     * Join the versions in a dash-separated-string, for use in file names.
     */
    val dashSeparated: String =
        versionsMap.entries
            .map { (key, value) -> "$key-$value" }
            .sorted()
            .joinToString("_")

    /**
     * Pretty display name, for use in JUnit test names.
     */
    val displayName: String =
        versionsMap.entries
            .map { (key, value) -> "$key: $value" }
            .sorted()
            .joinToString(", ")

    /**
     * Compares every version, in order.
     */
    override fun compareTo(other: TestedVersions): Int {
        versionsMap.entries
            .sortedBy { it.key }
            .forEach { (key, thisVersion) ->
                val otherVersion = other.versionsMap[key] ?: return@forEach
                val cmp = thisVersion.compareTo(otherVersion)
                if (cmp != 0) {
                    return cmp
                }
            }
        return 0
    }

    companion object {

        /** All possible Kotlin Gradle Plugin versions that could be tested. */
        private val allKgpVersions = listOf(
            "1.9.0", // required for Compose 1.5.1
            "1.9.23", // required for Compose 1.5.12
            "1.9.24", // required for Compose 1.5.14
            "1.9.25",
            "2.0.20",
            "2.0.21",
        )

        /** All possible Android Gradle Plugin versions that could be tested. */
        private val allAgpVersions = listOf(
            "7.4.2",
            "8.7.1",
        )

        /** Gradle versions to test. */
        private val allGradleVersions = listOf(
            "7.6.4",
            "8.10.2",
        )

        private val dokkaVersionOverride: String? by optionalSystemProperty()
        private val dokkaVersion: String by systemProperty { dokkaVersionOverride ?: it }

        /** Dokka Gradle Plugin versions to test. */
        private val allDokkaGradlePluginVersions = listOf(
            dokkaVersion,
        )

        /** Versions of the `org.jetbrains.compose` plugins */
        private val allComposeGradlePluginVersions = listOf(
            // https://github.com/JetBrains/compose-multiplatform/blob/c52b4a92b94714424dfdd73f6f91a55fec359a69/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/ComposeCompilerCompatibility.kt#L6
            "1.7.0",
        )

        /**
         * Generate all testable versions.
         *
         * Filter out incompatible versions (see [TestedVersions.isValid]).
         *
         * Only test the latest patch versions.
         * (E.g. Don't test both KGP 1.9.24 and 1.9.25, only test the latest version.)
         */
        private val allTestedVersions: Set<TestedVersions> = sequence {
            allGradleVersions.forEach { gradle ->
                allKgpVersions.forEach { kgp ->
                    allAgpVersions.forEach { agp ->
                        allDokkaGradlePluginVersions.forEach { dgp ->
                            allComposeGradlePluginVersions.forEach { composeGradlePlugin ->
                                yield(
                                    TestedVersions(
                                        kgp = kgp,
                                        gradle = gradle,
                                        dgp = dgp,
                                        agp = agp,
                                        composeGradlePlugin = composeGradlePlugin,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
            .filter { it.isValid() }

            // Group by major and minor version, and only select the highest patch version.
            // E.g. if the list contains both KGP 1.9.24 and 1.9.25, then remove 1.9.24.
            .groupingBy { it.majorAndMinorVersions }
            .reduce { _, current: TestedVersions, other: TestedVersions ->
                maxOf(current, other)
            }
            .values

            .sorted()
            .toSet()

        /**
         * Create a list with all possible combinations of tested versions.
         *
         * Versions with the same major and minor version but different patch versions will be removed.
         */
        fun testedVersions(
            isComposeTest: Boolean,
            isAndroidTest: Boolean,
        ): Sequence<TestedVersions> {
            return allTestedVersions
                .asSequence()
                .filter { if (isAndroidTest) it.agp != null else true }
                .filter { if (isComposeTest) it.composeGradlePlugin != null else true }
        }
    }
}
