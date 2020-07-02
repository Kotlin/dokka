package org.jetbrains.dokka.it.gradle

import org.gradle.util.GradleVersion

data class BuildVersions(
    val gradleVersion: GradleVersion,
    val kotlinVersion: String
) {
    constructor(
        gradleVersion: String,
        kotlinVersion: String
    ) : this(
        gradleVersion = GradleVersion.version(gradleVersion),
        kotlinVersion = kotlinVersion
    )
}
