/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

internal fun TestOutputWriter.navigationHtml(): Element = contents.getValue("navigation.html").let { Jsoup.parse(it) }

internal fun TestOutputWriter.pagesJson(): List<SearchRecord> = jacksonObjectMapper().readValue(contents.getValue("scripts/pages.json"))

internal fun Elements.selectNavigationGrid(): Element {
    return this.select("div.overview").select("span.nav-link-grid").single()
}
