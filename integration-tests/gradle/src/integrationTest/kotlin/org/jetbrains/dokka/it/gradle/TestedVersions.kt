package org.jetbrains.dokka.it.gradle

internal object TestedVersions {

    val JVM =
        BuildVersions.permutations(
            gradleVersions = listOf("7.3", "6.9"),
            kotlinVersions = listOf("1.6.10", "1.5.31", "1.4.32"),
        ) + BuildVersions.permutations(
            gradleVersions = listOf(*ifExhaustive("7.0", "6.1.1")),
            kotlinVersions = listOf(*ifExhaustive("1.6.0", "1.5.0", "1.4.0"))
        )

    /**
     * Starting with version 7, major android gradle plugin versions are aligned
     * with major gradle versions, i.e android plugin 7.0.0 will not work with gradle 6.9
     */
    val ANDROID =
        BuildVersions.permutations(
            gradleVersions = listOf("7.3", *ifExhaustive("7.0")),
            kotlinVersions = listOf("1.6.10", "1.5.31", "1.4.32"),
            androidGradlePluginVersions = listOf("7.0.0")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("6.9", *ifExhaustive("6.1.1", "5.6.4")),
            kotlinVersions = listOf("1.6.10", "1.5.31", "1.4.32"),
            androidGradlePluginVersions = listOf("4.0.0", *ifExhaustive("3.6.3"))
        )

    /**
     * MPP projects may not support latest gradle releases
     * straight away and will lag behind for some time
     */
    val MPP =
        BuildVersions.permutations(
            gradleVersions = listOf("7.1", "6.9"),
            kotlinVersions = listOf("1.6.10", "1.5.31", "1.4.32"),
        ) + BuildVersions.permutations(
            gradleVersions = listOf(*ifExhaustive("7.0", "6.1.1")),
            kotlinVersions = listOf(*ifExhaustive("1.6.0", "1.5.0", "1.4.0"))
        )
}