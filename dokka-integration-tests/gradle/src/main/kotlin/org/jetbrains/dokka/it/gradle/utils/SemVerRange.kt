/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.utils

/**
 * A range of [SemVer] versions.
 */
data class SemVerRange(
    private val min: SemVer?,
    private val minIsInclusive: Boolean,
    private val max: SemVer?,
    private val maxIsInclusive: Boolean,
) {
    operator fun contains(version: SemVer): Boolean {
        val version = SemVer("${version.major}.${version.minor}.${version.patch}")

        if (min != null) {
            if (minIsInclusive) {
                if (version < min) return false
            } else {
                if (version <= min) return false
            }
        }
        if (max != null) {
            if (maxIsInclusive) {
                if (version > max) return false
            } else {
                if (version >= max) return false
            }
        }
        return true
    }

    override fun toString(): String =
        buildString {
            if (minIsInclusive) {
                append('[')
            } else {
                append('(')
            }
            if (min != null) {
                append(min)
            }
            append(",")
            if (max != null) {
                append(" ")
                append(max)
            }
            if (maxIsInclusive) {
                append(']')
            } else {
                append(')')
            }
        }

    companion object {
        /**
         * Parse a Maven style version range.
         *
         * See https://docs.gradle.org/current/userguide/dependency_versions.html#sec:maven-style-range
         *
         * Returns `null` if the [range] is blank.
         */
        internal fun parseOrNullIfBlank(range: String): SemVerRange? {
            if (range.isBlank()) return null

            val firstChar = range.first()
            val lastChar = range.last()
            require(firstChar == '(' || firstChar == '[' || firstChar == ']') {
                "Invalid range start character: $firstChar. Must be one of: (, [, or ]"
            }
            require(lastChar == ')' || lastChar == ']' || lastChar == '[') {
                "Invalid range end character: $lastChar. Must be one of: ), ], or ["
            }

            val minIsInclusive = firstChar == '['
            val maxIsInclusive = lastChar == ']'

            val versions = range.drop(1).dropLast(1)

            require("," in versions) {
                "Invalid range: $range. Must contain a comma separator."
            }

            val (min, max) = versions.split(",").map {
                if (it.isBlank()) null
                else SemVer(it.trim())
            }

            return SemVerRange(
                min = min,
                max = max,
                minIsInclusive = minIsInclusive,
                maxIsInclusive = maxIsInclusive,
            )
        }
    }
}
