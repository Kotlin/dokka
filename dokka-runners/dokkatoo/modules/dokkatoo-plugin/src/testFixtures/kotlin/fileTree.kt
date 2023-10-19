package org.jetbrains.dokka.dokkatoo.utils

import java.io.File
import java.nio.file.Path

// based on https://gist.github.com/mfwgenerics/d1ec89eb80c95da9d542a03b49b5e15b
// context: https://kotlinlang.slack.com/archives/C0B8MA7FA/p1676106647658099

fun Path.toTreeString(): String = toFile().toTreeString()

fun File.toTreeString(): String = when {
  isDirectory -> name + "/\n" + buildTreeString(this)
  else        -> name
}

private fun buildTreeString(
  dir: File,
  margin: String = "",
): String {
  val entries = dir.listDirectoryEntries()

  return entries.joinToString("\n") { entry ->
    val (currentPrefix, nextPrefix) = when (entry) {
      entries.last() -> PrefixPair.LAST_ENTRY
      else           -> PrefixPair.INTERMEDIATE
    }

    buildString {
      append("$margin${currentPrefix}${entry.name}")

      if (entry.isDirectory) {
        append("/")
        if (entry.countDirectoryEntries() > 0) {
          append("\n")
        }
        append(buildTreeString(entry, margin + nextPrefix))
      }
    }
  }
}

private fun File.listDirectoryEntries(): Sequence<File> =
  walkTopDown().maxDepth(1).filter { it != this@listDirectoryEntries }


private fun File.countDirectoryEntries(): Int =
  listDirectoryEntries().count()

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
