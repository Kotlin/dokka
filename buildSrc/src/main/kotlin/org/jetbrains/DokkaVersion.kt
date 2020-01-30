package org.jetbrains

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate

fun Project.configureDokkaVersion(): String {
    var dokka_version: String? by this.extra
    if (dokka_version == null) {
        val buildNumber = System.getenv("BUILD_NUMBER")
        val dokka_version_base: String by this
        dokka_version = dokka_version_base + if (buildNumber == null || System.getenv("FORCE_SNAPSHOT") != null) {
            "-SNAPSHOT"
        } else {
            "-$buildNumber"
        }
    }
    return dokka_version!!
}