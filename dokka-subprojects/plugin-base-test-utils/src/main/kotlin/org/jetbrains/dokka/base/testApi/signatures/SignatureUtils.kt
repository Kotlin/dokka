/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import utils.Tag
import utils.TestOutputWriter

public fun TestOutputWriter.renderedContent(path: String = "root/example.html"): Element =
    contents.getValue(path).let { Jsoup.parse(it) }.select("#content")
        .single()

public fun Element.signature(): Elements = select("div.symbol.monospace")
public fun Element.tab(tabName: String): Elements = select("div[data-togglable=\"$tabName\"]")
public fun Element.firstSignature(): Element = signature().first() ?: throw NoSuchElementException("No signature found")
public fun Element.lastSignature(): Element = signature().last() ?: throw NoSuchElementException("No signature found")

public class Parameters(vararg matchers: Any) : Tag("span", *matchers, expectedClasses = listOf("parameters"))
public class Parameter(vararg matchers: Any) : Tag("span", *matchers, expectedClasses = listOf("parameter"))
