package org.jetbrains.dokka.pages

import org.jetbrains.dokka.model.properties.ExtraProperty

class SimpleAttr(val extraKey: String, val extraValue: String) : ExtraProperty<ContentNode> {
    data class SimpleAttrKey(val key: String) : ExtraProperty.Key<ContentNode, SimpleAttr>
    override val key: ExtraProperty.Key<ContentNode, SimpleAttr> = SimpleAttrKey(extraKey)

    companion object {
        fun header(value: String) = SimpleAttr("data-togglable", value)
    }
}

data class ContentTab(val text: ContentText, val overriddenToggleTarget: List<String>)
class ExtraTabs(val tabs: List<ContentTab>) : ExtraProperty<ContentNode> {
    companion object : ExtraProperty.Key<ContentNode, ExtraTabs>
    override val key: ExtraProperty.Key<ContentNode, ExtraTabs> = ExtraTabs
}

object HtmlContent : ExtraProperty<ContentNode>, ExtraProperty.Key<ContentNode, HtmlContent> {
    override val key: ExtraProperty.Key<ContentNode, *> = this
}
