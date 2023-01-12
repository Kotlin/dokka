package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentStyle

enum class TabbedContentType {
    TYPE, CONSTRUCTOR, FUNCTION, PROPERTY, ENTRY, EXTENSION
}

fun TabbedContentType.toExtra() = TabbedContent(type = this)

/**
 * TODO fix name clash with [ContentStyle.TabbedContent]
 */
class TabbedContent(val type: TabbedContentType) : ExtraProperty<ContentNode> {
    companion object : ExtraProperty.Key<ContentNode, TabbedContent>
    override val key: ExtraProperty.Key<ContentNode, TabbedContent> = TabbedContent
}

data class ContentTab(
    val name: String,
    val contentTypes: List<TabbedContentType>
)

class ContentTabs(val tabs: List<ContentTab>) : ExtraProperty<ContentNode> {
    companion object : ExtraProperty.Key<ContentNode, ContentTabs>
    override val key: ExtraProperty.Key<ContentNode, ContentTabs> = ContentTabs
}
