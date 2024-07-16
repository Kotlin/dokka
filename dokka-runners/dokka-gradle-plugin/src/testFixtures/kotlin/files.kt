package dev.adamko.dokkatoo.utils

import java.io.File
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.walk


fun File.copyInto(directory: File, overwrite: Boolean = false) =
  copyTo(directory.resolve(name), overwrite = overwrite)


fun Path.listRelativePathsMatching(predicate: (Path) -> Boolean): List<String> {
  val basePath = this.invariantSeparatorsPathString
  return walk()
    .filter(predicate)
    .map { it.invariantSeparatorsPathString.substringAfter(basePath).removePrefix("/") }
    .toList()
    .sorted()
}
