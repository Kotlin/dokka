/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

public fun Element.match(vararg matchers: Any, ignoreSpanWithTokenStyle:Boolean = false): Unit =
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

public open class Tag(
    public val name: String,
    public vararg val matchers: Any,
    public val expectedClasses: List<String> = emptyList()
)
public class Div(vararg matchers: Any) : Tag("div", *matchers)
public class P(vararg matchers: Any) : Tag("p", *matchers)
public class Span(vararg matchers: Any) : Tag("span", *matchers)
public class A(vararg matchers: Any) : Tag("a", *matchers)
public class B(vararg matchers: Any) : Tag("b", *matchers)
public class I(vararg matchers: Any) : Tag("i", *matchers)
public class STRIKE(vararg matchers: Any) : Tag("strike", *matchers)
public class BlockQuote(vararg matchers: Any) : Tag("blockquote", *matchers)
public class Dl(vararg matchers: Any) : Tag("dl", *matchers)
public class Dt(vararg matchers: Any) : Tag("dt", *matchers)
public class Dd(vararg matchers: Any) : Tag("dd", *matchers)
public class Var(vararg matchers: Any) : Tag("var", *matchers)
public class U(vararg matchers: Any) : Tag("u", *matchers)
public object Wbr : Tag("wbr")
public object Br : Tag("br")

public fun Tag.withClasses(vararg classes: String): Tag = Tag(name, *matchers, expectedClasses = classes.toList())

private fun Any.accepts(n: Node, ignoreSpan:Boolean = true) {
    when (this) {
        is String -> assert(n is TextNode && n.text().trim() == this.trim()) { "\"$this\" expected but found: $n" }
        is Tag -> {
            check(n is Element) { "Expected node to be Element: $n" }
            assert(n.tagName() == name) { "Tag \"$name\" expected but found: \"$n\"" }
            expectedClasses.forEach {
                assert(n.hasClass(it)) { "Expected to find class \"$it\" for tag \"$name\", found: ${n.classNames()}" }
            }
            if (matchers.isNotEmpty()) n.match(*matchers, ignoreSpanWithTokenStyle = ignoreSpan)
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
