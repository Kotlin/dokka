/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import io.kotest.assertions.fail
import java.nio.file.Path
import kotlin.io.path.*


/**
 * Compare the contents of this directory with that of [path].
 *
 * Only files will be compared, directories are ignored.
 */
infix fun Path.shouldBeDirectoryWithSameContentAs(path: Path) {
    val differences = describeFileDifferences(this, path)
    if (differences.isNotEmpty()) {
        fail(differences)
    }
}


/**
 * Build a string that describes the differences between [expectedDir] and [actualDir].
 *
 * Both the location and content of files is compared.
 * Only files are compared, directories are excluded.
 *
 * If the string is empty then no differences were detected.
 */
private fun describeFileDifferences(
    expectedDir: Path,
    actualDir: Path,
): String = buildString {
    if (!expectedDir.isDirectory()) {
        appendLine("expectedDir '$expectedDir' is not a directory (exists:${expectedDir.exists()}, file:${expectedDir.isRegularFile()})")
        return@buildString
    }
    if (!actualDir.isDirectory()) {
        appendLine("actualDir '$actualDir' is not a directory (exists:${actualDir.exists()}, file:${actualDir.isRegularFile()})")
        return@buildString
    }

    // Collect all files from directories recursively
    val expectedFiles = expectedDir.walk().filter { it.isRegularFile() }.map { it.relativeTo(expectedDir) }.toSet()
    val actualFiles = actualDir.walk().filter { it.isRegularFile() }.map { it.relativeTo(actualDir) }.toSet()

    // Check for files present in one directory but not the other
    val onlyInExpected = expectedFiles - actualFiles
    val onlyInActual = actualFiles - expectedFiles

    if (onlyInExpected.isNotEmpty()) {
        appendLine("actualDir is missing ${onlyInExpected.size} files:")
        appendLine(onlyInExpected.joinToFormattedList())
    }
    if (onlyInActual.isNotEmpty()) {
        appendLine("actualDir has ${onlyInActual.size} unexpected files:")
        appendLine(onlyInActual.joinToFormattedList())
    }

    // Compare contents of files that are present in both directories
    val commonFiles = onlyInExpected intersect onlyInActual

    commonFiles
        .sorted()
        .forEach { relativePath ->
            val expectedFile = expectedDir.resolve(relativePath)
            val actualFile = actualDir.resolve(relativePath)

            val expectedLines = expectedFile.readLines()
            val actualLines = actualFile.readLines()

            val patch = DiffUtils.diff(expectedLines, actualLines)

            if (patch.deltas.isNotEmpty()) {

                val diff = UnifiedDiffUtils.generateUnifiedDiff(
                    /* originalFileName = */ expectedFile.toString(),
                    /* revisedFileName = */ actualFile.toString(),
                    /* originalLines = */ expectedLines,
                    /* patch = */ patch,
                    /* contextSize = */ 3,
                )

                appendLine("\t${relativePath.invariantSeparatorsPathString} has ${diff.size} differences in content:")
                appendLine(diff.joinToString("\n", limit = 3).prependIndent("\t"))
            }
        }
}


/**
 * Pretty print files as a list.
 */
private fun Collection<Path>.joinToFormattedList(limit: Int = 10): String =
    joinToString("\n", limit = limit) { "\t- ${it.invariantSeparatorsPathString}" }
