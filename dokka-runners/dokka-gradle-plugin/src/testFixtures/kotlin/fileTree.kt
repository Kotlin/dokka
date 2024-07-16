package dev.adamko.dokkatoo.utils

import java.io.File
import java.nio.file.Path

// based on https://gist.github.com/mfwgenerics/d1ec89eb80c95da9d542a03b49b5e15b
// context: https://kotlinlang.slack.com/archives/C0B8MA7FA/p1676106647658099


fun Path.toTreeString(
  fileFilter: FileFilter = FileFilter { true },
): String =
  toFile().toTreeString(fileFilter = fileFilter)


fun File.toTreeString(
  fileFilter: FileFilter = FileFilter { true },
): String = when {
  isDirectory -> name + "/\n" + buildTreeString(dir = this, fileFilter = fileFilter)
  else        -> name
}


/**
 * Optionally include/exclude files. Directories will always be included.
 */
fun interface FileFilter {
  operator fun invoke(file: File): Boolean
}


private fun FileFilter.matches(file: File): Boolean =
  if (file.isDirectory) {
    // don't include directories that have no matches
    file.walk().any { it.isFile && invoke(it) }
  } else {
    invoke(file)
  }


private fun buildTreeString(
  dir: File,
  fileFilter: FileFilter = FileFilter { true },
  margin: String = "",
): String {
  val entries = dir.listDirectoryEntries()
    .filter { file -> fileFilter.matches(file) }

  return entries.joinToString("\n") { entry ->
    val (currentPrefix, nextPrefix) = when (entry) {
      entries.last() -> PrefixPair.LAST_ENTRY
      else           -> PrefixPair.INTERMEDIATE
    }

    buildString {
      append("$margin${currentPrefix}${entry.name}")

      if (entry.isDirectory) {
        append("/")
        if (entry.countDirectoryEntries(fileFilter) > 0) {
          append("\n")
        }
        append(buildTreeString(entry, fileFilter, margin + nextPrefix))
      }
    }
  }
}

private fun File.listDirectoryEntries(): Sequence<File> =
  walkTopDown()
    .maxDepth(1)
    .filter { it != this@listDirectoryEntries }
    .sortedWith(FileSorter)


private fun File.countDirectoryEntries(
  fileFilter: FileFilter,
): Int =
  listDirectoryEntries()
    .filter { file -> fileFilter.matches(file) }
    .count()


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


/**
 * Directories before files, otherwise sort by filename.
 */
private object FileSorter : Comparator<File> {
  override fun compare(o1: File, o2: File): Int {
    return when {
      o1.isDirectory && o2.isFile -> -1 // directories before files
      o1.isFile && o2.isDirectory -> +1 // files after directories
      else                        -> o1.name.compareTo(o2.name)
    }
  }
}
