package dev.adamko.dokkatoo.utils

import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.file.shouldBeADirectory
import io.kotest.matchers.file.shouldHaveSameContentAs
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun Path.shouldHaveSameStructureAs(path: Path, skipEmptyDirs: Boolean) {
  if (skipEmptyDirs) {
    toFile().shouldHaveSameStructureAs2(path.toFile(), ::isNotEmptyDir, ::isNotEmptyDir)
  } else {
    toFile().shouldHaveSameStructureAs2(path.toFile())
  }
}

fun Path.shouldHaveSameStructureAndContentAs(path: Path, skipEmptyDirs: Boolean) {
  if (skipEmptyDirs) {
    toFile().shouldHaveSameStructureAndContentAs2(path.toFile(), ::isNotEmptyDir, ::isNotEmptyDir)
  } else {
    toFile().shouldHaveSameStructureAndContentAs2(path.toFile())
  }
}

private fun isNotEmptyDir(file: File): Boolean =
  file.isFile || Files.newDirectoryStream(file.toPath()).use { it.count() } > 0


private fun File.shouldHaveSameStructureAs2(
  file: File,
  filterLhs: (File) -> Boolean = { false },
  filterRhs: (File) -> Boolean = { false },
) {
  shouldHaveSameStructureAndContentAs2(
    file,
    filterLhs = filterLhs,
    filterRhs = filterRhs
  ) { expect, actual ->
    val expectPath = expect.invariantSeparatorsPath.removePrefix(expectParentPath)
    val actualPath = actual.invariantSeparatorsPath.removePrefix(actualParentPath)
    expectPath shouldBe actualPath
  }
}

fun File.shouldHaveSameStructureAndContentAs2(
  file: File,
  filterLhs: (File) -> Boolean = { false },
  filterRhs: (File) -> Boolean = { false },
) {
  shouldHaveSameStructureAndContentAs2(
    file,
    filterLhs = filterLhs,
    filterRhs = filterRhs
  ) { expect, actual ->
    val expectPath = expect.invariantSeparatorsPath.removePrefix(expectParentPath)
    val actualPath = actual.invariantSeparatorsPath.removePrefix(actualParentPath)
    expectPath shouldBe actualPath

    expect.shouldHaveSameContentAs(actual)
  }
}


private fun File.shouldHaveSameStructureAndContentAs2(
  file: File,
  filterLhs: (File) -> Boolean = { false },
  filterRhs: (File) -> Boolean = { false },
  fileAssert: FileAsserter,
) {
  val expectFiles = this.walkTopDown().filter(filterLhs).toList()
  val actualFiles = file.walkTopDown().filter(filterRhs).toList()

  expectFiles shouldBeSameSizeAs actualFiles

  val assertContext = FileAsserter.Context(
    expectParentPath = this.invariantSeparatorsPath,
    actualParentPath = file.invariantSeparatorsPath,
  )

  expectFiles.zip(actualFiles) { expect, actual ->
    when {
      expect.isDirectory -> actual.shouldBeADirectory()
      expect.isFile      -> {
        with(fileAssert) {
          assertContext.assert(expect, actual)
        }
      }

      else               -> error("There is an unexpected error analyzing file trees. Failed to determine filetype of $expect")
    }
  }
}


private fun interface FileAsserter {

  data class Context(
    val expectParentPath: String,
    val actualParentPath: String,
  )

  fun Context.assert(expect: File, actual: File)
}
