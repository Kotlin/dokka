package org.jetbrains.dokka.base.templating

data class SearchRecord(
    val name: String,
    val description: String? = null,
    val location: String,
    val searchKeys: List<String> = listOf(name)
) {
    companion object
}
data class AddToSearch(val moduleName: String, val elements: List<SearchRecord>): Command