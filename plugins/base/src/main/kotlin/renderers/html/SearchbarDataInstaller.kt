package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.pages.ContentPage

typealias Json = String

data class SearchRecord(val name: String, val description: String? = null, val location: String, val searchKeys: List<String> = listOf(name)) {
    companion object { }
}

interface SearchbarDataInstaller {
    fun processPage(page: ContentPage, link: String)

    fun createSearchRecord(name: String, description: String?, location: String, searchKeys: List<String>): SearchRecord

    fun generatePagesList(): Json
}
