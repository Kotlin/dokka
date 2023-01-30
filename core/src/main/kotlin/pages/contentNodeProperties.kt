package org.jetbrains.dokka.pages

import org.jetbrains.dokka.model.properties.ExtraProperty

class SimpleAttr(val extraKey: String, val extraValue: String) : ExtraProperty<ContentNode> {
    data class SimpleAttrKey(val key: String) : ExtraProperty.Key<ContentNode, SimpleAttr>
    override val key: ExtraProperty.Key<ContentNode, SimpleAttr> = SimpleAttrKey(extraKey)

}

object HtmlInvisibleExtra : ExtraProperty<ContentNode>, ExtraProperty.Key<ContentNode, HtmlInvisibleExtra> {
    override val key: ExtraProperty.Key<ContentNode, *> = this
}

const val TOGGLEABLE_CONTENT_TYPE_ATTR = "data-togglable"
enum class BasicToggleableContentType : ToggleableContentType {
    TYPE, CONSTRUCTOR, FUNCTION, PROPERTY, ENTRY, EXTENSION, INHERITED_FUNCTION, INHERITED_PROPERTY,
    INVISIBLE
}

interface ToggleableContentType
class ToggleableContentTypeExtra(val value: ToggleableContentType) : ExtraProperty<ContentNode> {
    companion object : ExtraProperty.Key<ContentNode, ToggleableContentTypeExtra>
    override val key: ExtraProperty.Key<ContentNode, ToggleableContentTypeExtra> = ToggleableContentTypeExtra
}

/**
 * @param text a tab name
 */
data class ContentTab(val text: ContentText, val toggleableContentTypes: List<ToggleableContentType>)

/**
 * Tabs for a content with [ContentStyle.TabbedContent].
 *
 * @see ContentStyle.TabbedContent]
 */
class ContentTabsExtra(val tabs: List<ContentTab>) : ExtraProperty<ContentNode> {
    companion object : ExtraProperty.Key<ContentNode, ContentTabsExtra>
    override val key: ExtraProperty.Key<ContentNode, ContentTabsExtra> = ContentTabsExtra
}

object HtmlContent : ExtraProperty<ContentNode>, ExtraProperty.Key<ContentNode, HtmlContent> {
    override val key: ExtraProperty.Key<ContentNode, *> = this
}
