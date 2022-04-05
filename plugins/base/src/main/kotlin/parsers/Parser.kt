package org.jetbrains.dokka.base.parsers

import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.model.doc.Suppress

abstract class Parser {

    abstract fun parseStringToDocNode(extractedString: String): DocTag

    abstract fun preparse(text: String): String

    open fun parse(text: String): DocumentationNode =
        DocumentationNode(jkdocToListOfPairs(preparse(text)).map { (tag, content) -> parseTagWithBody(tag, content) })

    open fun parseTagWithBody(tagName: String, content: String): TagWrapper =
        when (tagName) {
            "description" -> Description(parseStringToDocNode(content))
            "author" -> Author(parseStringToDocNode(content))
            "version" -> Version(parseStringToDocNode(content))
            "since" -> Since(parseStringToDocNode(content))
            "see" -> See(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' '),
                null
            )
            "param" -> Param(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' ')
            )
            "property" -> Property(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' ')
            )
            "return" -> Return(parseStringToDocNode(content))
            "constructor" -> Constructor(parseStringToDocNode(content))
            "receiver" -> Receiver(parseStringToDocNode(content))
            "throws", "exception" -> Throws(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' '),
                null
            )
            "deprecated" -> Deprecated(parseStringToDocNode(content))
            "sample" -> Sample(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' ')
            )
            "suppress" -> Suppress(parseStringToDocNode(content))
            else -> CustomTagWrapper(parseStringToDocNode(content), tagName)
        }

    private fun jkdocToListOfPairs(javadoc: String): List<Pair<String, String>> =
        "description $javadoc"
            .splitIgnoredInsideBackticks("\n@")
            .map { content ->
                val contentWithEscapedAts = content.replace("\\@", "@")
                val (tag, body) = contentWithEscapedAts.split(" ", limit = 2)
                tag to body
            }

    private fun CharSequence.splitIgnoredInsideBackticks(delimiter: String): List<String> {
        var countOfBackticks = 0
        var countOfBackticksInOpeningFence = 0

        var isInCode = false
        val result = mutableListOf<String>()
        var rangeStart = 0
        var rangeEnd = 0
        var currentOffset = 0
        while (currentOffset < length) {

            if (get(currentOffset) == '`') {
                countOfBackticks++
            } else {
                if (isInCode) {
                    // The closing code fence must be at least as long as the opening fence
                    isInCode = countOfBackticks < countOfBackticksInOpeningFence
                } else {
                    if (countOfBackticks > 0) {
                        isInCode = true
                        countOfBackticksInOpeningFence = countOfBackticks
                    }
                }
                countOfBackticks = 0
            }
            if (!isInCode && startsWith(delimiter, currentOffset)) {
                result.add(substring(rangeStart, rangeEnd))
                currentOffset += delimiter.length
                rangeStart = currentOffset
                rangeEnd = currentOffset
                continue
            }

            ++rangeEnd
            ++currentOffset
        }
        result.add(substring(rangeStart, rangeEnd))
        return result
    }

}
