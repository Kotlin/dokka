/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.util.GradleVersion

public data class BuildVersions(
    val gradleVersion: GradleVersion,
    val kotlinVersion: String,
    val androidGradlePluginVersion: String? = null,
) {
    public constructor(
        gradleVersion: String,
        kotlinVersion: String,
        androidGradlePluginVersion: String? = null
    ) : this(
        gradleVersion = GradleVersion.version(gradleVersion),
        kotlinVersion = kotlinVersion,
        androidGradlePluginVersion = androidGradlePluginVersion
    )

    override fun toString(): String {
        return buildString {
            append("Gradle ${gradleVersion.version}, Kotlin $kotlinVersion")
            if (androidGradlePluginVersion != null) {
                append(", Android $androidGradlePluginVersion")
            }
        }
    }

    public companion object {
        public fun permutations(
            gradleVersions: List<String>,
            kotlinVersions: List<String>,
            androidGradlePluginVersions: List<String?> = listOf(null)
        ): List<BuildVersions> {
            return gradleVersions.distinct().flatMap { gradleVersion ->
                kotlinVersions.distinct().flatMap { kotlinVersion ->
                    androidGradlePluginVersions.distinct().map { androidVersion ->
                        BuildVersions(
                            gradleVersion = gradleVersion,
                            kotlinVersion = kotlinVersion,
                            androidGradlePluginVersion = androidVersion
                        )
                    }
                }
            }
        }
    }
}
