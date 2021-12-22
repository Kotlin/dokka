package org.jetbrains.dokka.it.gradle

internal object TestedVersions {

    val BASE =
        BuildVersions.permutations(
            gradleVersions = listOf("7.3", "6.9"),
            kotlinVersions = listOf("1.6.10", "1.5.31", "1.4.32"),
        ) + BuildVersions.permutations(
            gradleVersions = listOf(*ifExhaustive("7.0", "6.1.1")),
            kotlinVersions = listOf(*ifExhaustive("1.6.0", "1.5.0", "1.4.0"))
        )

    val ANDROID = listOf("7.0.0", "4.0.0", *ifExhaustive("3.6.3"))

    fun getBaseWithAndroid() = BASE.flatMap { buildVersions ->
        ANDROID.map { buildVersions.copy(androidGradlePluginVersion = it) }
    }
}