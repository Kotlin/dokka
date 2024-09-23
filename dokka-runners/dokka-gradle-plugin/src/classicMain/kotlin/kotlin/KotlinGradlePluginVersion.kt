/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
    val versionParts = fullVersionString.split(".", limit = 3).map { it.takeWhile { char -> char.isDigit() } }

    if (versionParts.size == 3 && versionParts.all { it.isNotEmpty() && it.all(Char::isDigit) }) {
        val (major, minor, patch) = versionParts.map { it.toInt() }
        return KotlinVersion(major, minor, patch)
    }

    return null
}
