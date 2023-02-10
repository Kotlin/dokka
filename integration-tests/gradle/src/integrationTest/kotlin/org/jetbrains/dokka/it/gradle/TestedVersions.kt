package org.jetbrains.dokka.it.gradle

internal object TestedVersions {

    val LATEST = BuildVersions("7.4.2", "1.8.10")

    /**
     * All supported Gradle/Kotlin versions, including [LATEST]
     *
     * [Kotlin/Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#kotlin)
     */
    val ALL_SUPPORTED =
        BuildVersions.permutations(
            gradleVersions = listOf("6.9"),
            kotlinVersions = listOf("1.7.20", "1.6.21", "1.5.31", "1.4.32"),
        ) + BuildVersions.permutations(
            gradleVersions = listOf(*ifExhaustive("7.0", "6.1.1")),
            kotlinVersions = listOf(*ifExhaustive("1.7.0", "1.6.0", "1.5.0", "1.4.0"))
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
            gradleVersions = listOf("7.4.2", *ifExhaustive("7.0")),
            kotlinVersions = listOf("1.7.20", "1.6.21", "1.5.31", "1.4.32"),
            androidGradlePluginVersions = listOf("7.2.0")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("6.9", *ifExhaustive("6.1.1", "5.6.4")),
            kotlinVersions = listOf("1.8.0", "1.7.0", "1.6.0", "1.5.0", "1.4.0"),
            androidGradlePluginVersions = listOf("4.0.0", *ifExhaustive("3.6.3"))
        ) + LATEST

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react
    val KT_REACT_WRAPPER_MAPPING = mapOf(
        "1.5.0" to "17.0.2-pre.204-kotlin-1.5.0",
        "1.6.0" to "17.0.2-pre.280-kotlin-1.6.0",
        "1.5.31" to "17.0.2-pre.265-kotlin-1.5.31",
        "1.6.21" to "18.0.0-pre.332-kotlin-1.6.21",
        "1.7.20" to "18.2.0-pre.391",
        "1.8.0" to "18.2.0-pre.467",
        "1.8.10" to "18.2.0-pre.490",
    )
}
