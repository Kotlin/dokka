package utils

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

fun Element.match(vararg matchers: Any): Unit =
    childNodes()
        .filter { it !is TextNode || it.text().isNotBlank() }
        .let { it.drop(it.size - matchers.size) }
        .zip(matchers)
        .forEach { (n, m) -> m.accepts(n) }

open class Tag(val name: String, vararg val matchers: Any)
class Div(vararg matchers: Any) : Tag("div", *matchers)
class P(vararg matchers: Any) : Tag("p", *matchers)
class Span(vararg matchers: Any) : Tag("span", *matchers)
class A(vararg matchers: Any) : Tag("a", *matchers)
class B(vararg matchers: Any) : Tag("b", *matchers)
class I(vararg matchers: Any) : Tag("i", *matchers)
class STRIKE(vararg matchers: Any) : Tag("strike", *matchers)
object Wbr : Tag("wbr")
private fun Any.accepts(n: Node) {
    when (this) {
        is String -> assert(n is TextNode && n.text().trim() == this.trim()) { "\"$this\" expected but found: $n" }
        is Tag -> {
            assert(n is Element && n.tagName() == name) { "Tag $name expected but found: $n" }
            if (n is Element && matchers.isNotEmpty()) n.match(*matchers)
        }
        else -> throw IllegalArgumentException("$this is not proper matcher")
    }
}