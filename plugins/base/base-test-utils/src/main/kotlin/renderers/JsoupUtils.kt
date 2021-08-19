package utils

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

fun Element.match(vararg matchers: Any, ignoreSpanWithTokenStyle:Boolean = false): Unit =
    childNodes()
        .let { list ->
            if(ignoreSpanWithTokenStyle) {
                list
                    .filterNot { it is Element && it.tagName() == "span" && it.attr("class").startsWith("token ") &&  it.childNodeSize() == 0}
                    .map { if(it is Element && it.tagName() == "span"
                        && it.attr("class").startsWith("token ")
                        && it.childNodeSize() == 1) it.childNode(0) else it }
                    .uniteConsecutiveTextNodes()
            } else list
        }
        .filter { (it !is TextNode || it.text().isNotBlank())}
        .let { it.drop(it.size - matchers.size) }
        .zip(matchers)
        .forEach { (n, m) -> m.accepts(n, ignoreSpan = ignoreSpanWithTokenStyle) }

open class Tag(val name: String, vararg val matchers: Any)
class Div(vararg matchers: Any) : Tag("div", *matchers)
class P(vararg matchers: Any) : Tag("p", *matchers)
class Span(vararg matchers: Any) : Tag("span", *matchers)
class A(vararg matchers: Any) : Tag("a", *matchers)
class B(vararg matchers: Any) : Tag("b", *matchers)
class I(vararg matchers: Any) : Tag("i", *matchers)
class STRIKE(vararg matchers: Any) : Tag("strike", *matchers)
object Wbr : Tag("wbr")
private fun Any.accepts(n: Node, ignoreSpan:Boolean = true) {
    when (this) {
        is String -> assert(n is TextNode && n.text().trim() == this.trim()) { "\"$this\" expected but found: $n" }
        is Tag -> {
            assert(n is Element && n.tagName() == name) { "Tag $name expected but found: $n" }
            if (n is Element && matchers.isNotEmpty()) n.match(*matchers, ignoreSpanWithTokenStyle = ignoreSpan)
        }
        else -> throw IllegalArgumentException("$this is not proper matcher")
    }
}
private fun List<Node>.uniteConsecutiveTextNodes(): MutableList<Node> {
    val resList = mutableListOf<Node>()
    var acc = StringBuilder()
    forEachIndexed { index, item ->
        if (item is TextNode) {
            acc.append(item.text())
            if (!(index + 1 < size && this[index + 1] is TextNode)) {
                resList.add(TextNode(acc.toString()))
                acc = StringBuilder()
            }
        } else resList.add(item)
    }
    return resList
 }