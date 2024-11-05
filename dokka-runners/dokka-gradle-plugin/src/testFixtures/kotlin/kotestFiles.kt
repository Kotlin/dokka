/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import io.kotest.assertions.fail
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.*


/**
 * Compare the contents of this directory with that of [path].
 *
 * Only files will be compared, directories are ignored.
 */
infix fun Path.shouldBeADirectoryWithSameContentAs(path: Path) {
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
    fun Path.allFiles(): Set<Path> =
        walk().filter { it.isRegularFile() }.map { it.relativeTo(this@allFiles) }.toSet()

    val expectedFiles = expectedDir.allFiles()
    val actualFiles = actualDir.allFiles()

    // Check for files present in one directory but not the other
    val onlyInExpected = expectedFiles - actualFiles
    val onlyInActual = actualFiles - expectedFiles

    if (onlyInExpected.isNotEmpty()) {
        appendLine("actualDir is missing ${onlyInExpected.size} files:")
        appendLine(onlyInExpected.sorted().joinToFormattedList())
    }
    if (onlyInActual.isNotEmpty()) {
        appendLine("actualDir has ${onlyInActual.size} unexpected files:")
        appendLine(onlyInActual.sorted().joinToFormattedList())
    }

    // Compare contents of files that are present in both directories
    val commonFiles = actualFiles intersect expectedFiles

    commonFiles
        .sorted()
        .forEach { relativePath ->
            val expectedFile = expectedDir.resolve(relativePath)
            val actualFile = actualDir.resolve(relativePath)

            val expectedLines = expectedFile.readByteLines()
            val actualLines = actualFile.readByteLines()

            val patch = DiffUtils.diff(expectedLines, actualLines)

            if (patch.deltas.isNotEmpty()) {
                appendLine("${relativePath.invariantSeparatorsPathString} has ${patch.deltas.size} differences in content:")

                val diff = UnifiedDiffUtils.generateUnifiedDiff(
                    /* originalFileName = */ expectedFile.relativeTo(expectedDir).invariantSeparatorsPathString,
                    /* revisedFileName = */ actualFile.relativeTo(actualDir).invariantSeparatorsPathString,
                    /* originalLines = */ expectedLines,
                    /* patch = */ patch,
                    /* contextSize = */ 3,
                )

                appendLine(diff.joinToString("\n").prependIndent())
            }
        }
}


/**
 * Pretty print files as a list.
 */
private fun Collection<Path>.joinToFormattedList(limit: Int = 10): String =
    joinToString("\n", limit = limit) { "  - ${it.invariantSeparatorsPathString}" }


/**
 * Read lines from a file, leniently.
 * Handles text and binary data.
 *
 * ([kotlin.io.path.readLines] blows up when it reads binary files.)
 */
private fun Path.readByteLines(): List<String> {
    try {
        inputStream().bufferedReader().use { reader ->
            return generateSequence { reader.readLine() }.toList()
        }
    } catch (e: Exception) {
        throw IOException("Could not read lines from $this", e)
    }
}
