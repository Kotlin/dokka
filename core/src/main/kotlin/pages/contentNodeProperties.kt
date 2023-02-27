package org.jetbrains.dokka.pages

import org.jetbrains.dokka.model.properties.ExtraProperty

class SimpleAttr(val extraKey: String, val extraValue: String) : ExtraProperty<ContentNode> {
    data class SimpleAttrKey(val key: String) : ExtraProperty.Key<ContentNode, SimpleAttr>
    override val key: ExtraProperty.Key<ContentNode, SimpleAttr> = SimpleAttrKey(extraKey)

}

enum class BasicTabbedContentType : TabbedContentType {
    TYPE, CONSTRUCTOR, FUNCTION, PROPERTY, ENTRY, EXTENSION_PROPERTY, EXTENSION_FUNCTION
}

/**
 * It is used only to mark content for tabs in HTML format
 */
interface TabbedContentType

/**
 * @see TabbedContentType
 */
class TabbedContentTypeExtra(val value: TabbedContentType) : ExtraProperty<ContentNode> {
    companion object : ExtraProperty.Key<ContentNode, TabbedContentTypeExtra>
    override val key: ExtraProperty.Key<ContentNode, TabbedContentTypeExtra> = TabbedContentTypeExtra
}

object HtmlContent : ExtraProperty<ContentNode>, ExtraProperty.Key<ContentNode, HtmlContent> {
    override val key: ExtraProperty.Key<ContentNode, *> = this
}
