package org.jetbrains.dokka.base.renderers.html

class JsSerializer {
    fun serialize(content: Iterable<SearchRecord>): String = content.joinToString(separator = ",", prefix = "[", postfix = "]") { serialize(it) }

    fun serialize(record: SearchRecord): String =
        """
            { name: "${record.name}", description: ${ record.description?.let { "\"$it\"" } ?: "null" }, location: "${record.location}", searchKeys: ${record.searchKeys.joinToString(prefix = "[", postfix = "]"){ "\"$it\""}}}
        """.trimIndent()
}