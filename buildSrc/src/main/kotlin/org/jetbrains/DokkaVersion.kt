package org.jetbrains

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate

fun Project.configureDokkaVersion(): String {
    var dokka_version: String? by this.extra
    if (dokka_version == null) {
        val dokka_version_base: String by this
        dokka_version = dokkaVersionFromBase(dokka_version_base)
    }
    return checkNotNull(dokka_version)
}

private fun dokkaVersionFromBase(baseVersion: String): String {
    val buildNumber = System.getenv("BUILD_NUMBER")
    val forceSnapshot = System.getenv("FORCE_SNAPSHOT") != null
    if (forceSnapshot || buildNumber == null) {
        return "$baseVersion-SNAPSHOT"
    }
    return "$baseVersion-$buildNumber"
}

val Project.dokkaVersion: String
    get() = configureDokkaVersion()

val Project.dokkaVersionType: DokkaVersionType?
    get() = DokkaVersionType.values().find { it.suffix.matches(dokkaVersion.substringAfter("-", "")) }
