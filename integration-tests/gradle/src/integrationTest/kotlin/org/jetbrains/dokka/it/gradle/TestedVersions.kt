package org.jetbrains.dokka.it.gradle

internal object TestedVersions {

    // Kotlin and Gradle compatibility matrix: https://kotlinlang.org/docs/gradle.html
    val BASE =
        BuildVersions.permutations(
            gradleVersions = listOf("7.5.1", "6.7.1"),
            kotlinVersions = listOf("1.7.20", "1.6.21", "1.5.31", "1.4.32"),
        ) + BuildVersions.permutations(
            gradleVersions = listOf(*ifExhaustive("7.0", "6.7.1")),
            kotlinVersions = listOf(*ifExhaustive("1.7.0", "1.6.0", "1.5.0", "1.4.0"))
        )

    // Starting with version 7, major android gradle plugin versions are aligned
    // with major gradle versions, i.e android plugin 7.0.0 will not work with gradle 6.9.
    // Android and Gradle plugins compatibility matrix: https://developer.android.com/studio/releases/gradle-plugin
    val ANDROID =
        BuildVersions.permutations(
            androidGradlePluginVersions = listOf("7.3.0"),
            gradleVersions = listOf("7.5.1"),
            kotlinVersions = listOf("1.7.20", "1.6.21", "1.5.31", "1.4.32"),
        ) + BuildVersions.permutations(
            androidGradlePluginVersions = listOf("4.2.0"),
            gradleVersions = listOf("6.7.1"),
            kotlinVersions = listOf("1.7.20", "1.6.21", "1.5.31", "1.4.32"),
        ) + BuildVersions.permutations(
            androidGradlePluginVersions = listOf(*ifExhaustive("3.6.4")),
            gradleVersions = listOf(*ifExhaustive("6.7.1")),
            kotlinVersions = listOf(*ifExhaustive("1.7.20", "1.6.21", "1.5.31", "1.4.32")),
        )

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react
    val KT_REACT_WRAPPER_MAPPING = mapOf(
        "1.5.0" to "17.0.2-pre.204-kotlin-1.5.0",
        "1.6.0" to "17.0.2-pre.280-kotlin-1.6.0",
        "1.5.31" to "17.0.2-pre.265-kotlin-1.5.31",
        "1.6.21" to "18.0.0-pre.332-kotlin-1.6.21",
        "1.7.20" to "18.2.0-pre.391",
    )
}
