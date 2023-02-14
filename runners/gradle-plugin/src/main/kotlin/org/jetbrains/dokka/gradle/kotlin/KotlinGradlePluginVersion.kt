package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

internal typealias KotlinGradlePluginVersion = KotlinVersion

internal fun Project.getKgpVersion(): KotlinGradlePluginVersion? = parseKotlinVersion(this.getKotlinPluginVersion())

/**
 * Accepts a full version string that contains the major, minor
 * and patch versions divided by dots, such as "1.7.10".
 *
 * Does NOT parse and store custom suffixes, so `1.8.20-RC2`
 * or `1.8.20-dev-42` will be viewed as `1.8.20`.
 */
internal fun parseKotlinVersion(fullVersionString: String): KotlinVersion? {
    val versionParts = fullVersionString
        .split(".", "-", limit = 4)
        .takeIf { parts -> parts.size >= 3 && parts.subList(0, 3).all { it.isNumeric() } }
        ?: return null

    return KotlinVersion(
        major = versionParts[0].toInt(),
        minor = versionParts[1].toInt(),
        patch = versionParts[2].toInt()
    )
}

private fun String.isNumeric() = this.isNotEmpty() && this.all { it.isDigit() }
