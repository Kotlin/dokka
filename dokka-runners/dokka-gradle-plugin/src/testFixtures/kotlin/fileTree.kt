/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import java.nio.file.Path
import kotlin.io.path.*

// based on https://gist.github.com/mfwgenerics/d1ec89eb80c95da9d542a03b49b5e15b
// context: https://kotlinlang.slack.com/archives/C0B8MA7FA/p1676106647658099


fun Path.toTreeString(
    fileFilter: FileFilter = FileFilter { true },
): String = when {
    isDirectory() -> name + "/\n" + buildTreeString(dir = this, fileFilter = fileFilter)
    else -> name
}


/**
 * Optionally include/exclude files. Directories will always be included.
 */
fun interface FileFilter {
    operator fun invoke(file: Path): Boolean
}


private fun FileFilter.matches(file: Path): Boolean =
    if (file.isDirectory()) {
        // don't include directories that have no matches
        file.walk().any { it.isRegularFile() && invoke(it) }
    } else {
        invoke(file)
    }


private fun buildTreeString(
    dir: Path,
    fileFilter: FileFilter = FileFilter { true },
    margin: String = "",
): String {
    val entries = dir.listDirectoryEntries()
        .filter { file -> fileFilter.matches(file) }

    return entries.joinToString("\n") { entry ->
        val (currentPrefix, nextPrefix) = when (entry) {
            entries.last() -> PrefixPair.LAST_ENTRY
            else -> PrefixPair.INTERMEDIATE
        }

        buildString {
            append("$margin${currentPrefix}${entry.name}")

            if (entry.isDirectory()) {
                append("/")
                if (entry.countDirectoryEntries(fileFilter) > 0) {
                    append("\n")
                }
                append(buildTreeString(entry, fileFilter, margin + nextPrefix))
            }
        }
    }
}


private fun Path.countDirectoryEntries(
    fileFilter: FileFilter,
): Int =
    listDirectoryEntries().count { file -> fileFilter.matches(file) }


private data class PrefixPair(
    /** The current entry should be prefixed with this */
    val currentPrefix: String,
    /** If the next item is a directory, it should be prefixed with this */
    val nextPrefix: String,
) {
    companion object {
        /** Prefix pair for a non-last directory entry */
        val INTERMEDIATE = PrefixPair("├── ", "│   ")

        /** Prefix pair for the last directory entry */
        val LAST_ENTRY = PrefixPair("└── ", "    ")
    }
}
