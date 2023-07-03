package utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

internal fun TestOutputWriter.navigationHtml(): Element = contents.getValue("navigation.html").let { Jsoup.parse(it) }

internal fun TestOutputWriter.pagesJson(fileName: String): List<SearchRecord> = jacksonObjectMapper().readValue(contents.getValue(fileName))

internal fun Elements.selectNavigationGrid(): Element {
    return this.select("div.overview").select("span.nav-link-grid").single()
}
