package signatures

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utils.Tag
import utils.TestOutputWriter

fun TestOutputWriter.renderedContent(path: String = "root/example.html") =
    contents.getValue(path).let { Jsoup.parse(it) }.select("#content")
        .single()

fun Element.signature() = select("div.symbol.monospace")
fun Element.firstSignature() = signature().first()

class Parameters(vararg matchers: Any) : Tag("span", *matchers, expectedClasses = listOf("parameters"))
class Parameter(vararg matchers: Any) : Tag("span", *matchers, expectedClasses = listOf("parameter"))