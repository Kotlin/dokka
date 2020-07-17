package org.jetbrains.dokka.pages

import org.jetbrains.dokka.model.properties.ExtraProperty

class SimpleAttr(val extraKey: String, val extraValue: String) : ExtraProperty<ContentNode> {
    data class SimpleAttrKey(val key: String) : ExtraProperty.Key<ContentNode, SimpleAttr>
    override val key: ExtraProperty.Key<ContentNode, SimpleAttr> = SimpleAttrKey(extraKey)

    companion object {
        fun header(value: String) = SimpleAttr("data-togglable", value)
    }
}
