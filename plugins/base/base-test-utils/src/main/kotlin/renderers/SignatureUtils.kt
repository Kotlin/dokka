package signatures

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import utils.TestOutputWriter

fun TestOutputWriter.renderedContent(path: String = "root/example.html"): Element =
    contents.getValue(path).let { Jsoup.parse(it) }.select("#content")
        .single()

fun Element.signature(): Elements = select("div.symbol.monospace")
fun Element.firstSignature(): Element = signature().first()