/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.utils

/**
 * A range of [SemVer] versions.
 *
 * Only compares using the major, minor and patch components of the versions.
 */
data class SemVerRange(
    private val minInclusive: SemVer?,
    private val maxExclusive: SemVer?,
) {
    init {
        require(minInclusive != null || maxExclusive != null) {
            "Invalid range: At least one bound must be specified. minInclusive=$minInclusive, maxExclusive=$maxExclusive"
        }
        if (minInclusive != null && maxExclusive != null) {
            require(minInclusive < maxExclusive) {
                "Invalid range: minInclusive=$minInclusive must be less than maxExclusive=$maxExclusive"
            }
        }
    }

    operator fun contains(version: SemVer): Boolean {
        val version = SemVer("${version.major}.${version.minor}.${version.patch}")

        if (minInclusive != null) {
            if (version < minInclusive) return false
        }
        if (maxExclusive != null) {
            if (version >= maxExclusive) return false
        }
        return true
    }

    companion object {
        internal fun from(
            min: String,
            max: String,
        ): SemVerRange {
            val min = if (min.isNotBlank()) SemVer(min) else null
            val max = if (max.isNotBlank()) SemVer(max) else null
            return SemVerRange(
                minInclusive = min,
                maxExclusive = max,
            )
        }
    }
}
