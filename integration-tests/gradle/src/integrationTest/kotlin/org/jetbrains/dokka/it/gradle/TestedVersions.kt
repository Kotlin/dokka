package org.jetbrains.dokka.it.gradle

internal object TestedVersions {

    val BASE =
        BuildVersions.permutations(
            gradleVersions = listOf("7.4.2", "6.9"),
            kotlinVersions = listOf("1.6.21", "1.5.31", "1.4.32"),
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
            gradleVersions = listOf("7.4.2", *ifExhaustive("7.0")),
            kotlinVersions = listOf("1.6.21", "1.5.31", "1.4.32"),
            androidGradlePluginVersions = listOf("7.1.2")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("6.9", *ifExhaustive("6.1.1", "5.6.4")),
            kotlinVersions = listOf("1.6.21", "1.5.31", "1.4.32"),
            androidGradlePluginVersions = listOf("4.0.0", *ifExhaustive("3.6.3"))
        )

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react
    val KT_REACT_WRAPPER_MAPPING = mapOf(
        "1.5.0" to "-Preact_version=17.0.2-pre.204-kotlin-1.5.0",
        "1.6.0" to "-Preact_version=17.0.2-pre.280-kotlin-1.6.0",
        "1.5.31" to "-Preact_version=17.0.2-pre.265-kotlin-1.5.31",
        "1.6.21" to "-Preact_version=18.0.0-pre.332-kotlin-1.6.21"
    )
}