package org.jetbrains.dokka.dokkatoo.utils

/** Replace all newlines with `\n`, so the String can be used in assertions cross-platform */
fun String.invariantNewlines(): String =
  lines().joinToString("\n")

fun Pair<String, String>.sideBySide(
  buffer: String = "   ",
): String {
  val (left, right) = this

  val leftLines = left.lines()
  val rightLines = right.lines()

  val maxLeftWidth = leftLines.maxOf { it.length }

  return (0..maxOf(leftLines.size, rightLines.size)).joinToString("\n") { i ->

    val leftLine = (leftLines.getOrNull(i) ?: "").padEnd(maxLeftWidth, ' ')
    val rightLine = rightLines.getOrNull(i) ?: ""

    leftLine + buffer + rightLine
  }
}
