package parsers

import model.doc.*
import model.doc.Deprecated
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag


abstract class Parser {

    abstract fun parseStringToDocNode(extractedString: String): DocNode
    abstract fun preparse(text: String): String

    fun parse(text: String): DocHeader {

        val list = jkdocToListOfPairs(preparse(text))

        val mappedList: List<DocType> = list.map {
            when(it.first) {
                "description"         -> Description(parseStringToDocNode(it.second))
                "author"              -> Author(parseStringToDocNode(it.second))
                "version"             -> Version(parseStringToDocNode(it.second))
                "since"               -> Since(parseStringToDocNode(it.second))
                "see"                 -> See(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '))
                "param"               -> Param(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '))
                "property"            -> Property(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '))
                "return"              -> Return(parseStringToDocNode(it.second))
                "constructor"         -> Constructor(parseStringToDocNode(it.second))
                "receiver"            -> Receiver(parseStringToDocNode(it.second))
                "throws", "exception" -> Throws(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '))
                "deprecated"          -> Deprecated(parseStringToDocNode(it.second))
                "sample"              -> Sample(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '))
                "suppress"            -> Suppress(parseStringToDocNode(it.second))
                else                  -> CustomTag(parseStringToDocNode(it.second), it.first)
            }
        }
        return DocHeader(mappedList)
    }

    private fun jkdocToListOfPairs(javadoc: String): List<Pair<String, String>> =
        "description $javadoc"
            .split("\n@")
            .map {
                it.substringBefore(' ') to it.substringAfter(' ')
            }
}