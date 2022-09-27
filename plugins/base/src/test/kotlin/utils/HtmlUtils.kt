package utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

internal fun TestOutputWriter.navigationHtml(): Element = contents.getValue("navigation.html").let { Jsoup.parse(it) }

internal fun Elements.selectNavigationGrid(): Element {
    return this.select("div.overview").select("span.nav-link-grid").single()
}
