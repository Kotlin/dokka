/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

class LatestTestedVersionsArgumentsProvider : TestedVersionsArgumentsProvider(listOf(TestedVersions.LATEST))
open class AllSupportedTestedVersionsArgumentsProvider : TestedVersionsArgumentsProvider(TestedVersions.ALL_SUPPORTED)

object TestedVersions {

    val LATEST = BuildVersions("8.14.3", "2.2.20")

    /**
     * All supported Gradle/Kotlin versions, including [LATEST]
     *
     * [Kotlin/Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#kotlin)
     */
    val ALL_SUPPORTED =
        BuildVersions.permutations(
            gradleVersions = listOf("7.6.3"),
            kotlinVersions = listOf("2.1.21", "2.0.21", "1.9.25"),
        ) + LATEST

    /**
     * Supported Android/Gradle/Kotlin versions, including [LATEST]
     *
     * Starting with version 7, major Android Gradle Plugin versions are aligned
     * with major Gradle versions, i.e AGP 7.X will only work with Gradle 7.X
     *
     * [AGP/Gradle compatibility matrix](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)
     */
    val ANDROID =
        BuildVersions.permutations(
            gradleVersions = listOf("8.4"),
            kotlinVersions = listOf("2.2.0", "2.1.21", "2.0.21"),
            androidGradlePluginVersions = listOf("8.3.0")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("7.6.3"),
            kotlinVersions = listOf("1.9.25"),
            androidGradlePluginVersions = listOf("7.4.2")
        )

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react
    val KT_REACT_WRAPPER_MAPPING = mapOf(
        "1.9.25" to "18.2.0-pre.682",
        "2.0.21" to "18.3.1-pre.758",
        "2.1.21" to "2025.6.2-19.1.0",
        "2.2.20" to "2025.10.3-19.2.0",
    )
}

abstract class TestedVersionsArgumentsProvider(private val buildVersions: List<BuildVersions>) : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return buildVersions.stream().map { Arguments.of(it) }
    }
}
