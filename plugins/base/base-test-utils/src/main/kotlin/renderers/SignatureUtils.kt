package signatures

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utils.TestOutputWriter

fun TestOutputWriter.renderedContent(path: String = "root/example.html") =
    contents.getValue(path).let { Jsoup.parse(it) }.select("#content")
        .single()

fun Element.signature() = select("div.symbol.monospace")
fun Element.firstSignature() = signature().first()