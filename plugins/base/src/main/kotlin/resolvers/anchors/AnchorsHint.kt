package org.jetbrains.dokka.base.resolvers.anchors

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

data class SymbolAnchorHint(val anchorName: String): ExtraProperty<ContentNode> {
    object SymbolAnchorHintKey : ExtraProperty.Key<ContentNode, SymbolAnchorHint>
    override val key: ExtraProperty.Key<ContentNode, SymbolAnchorHint> = SymbolAnchorHintKey
    companion object: ExtraProperty.Key<ContentNode, SymbolAnchorHint> {
        fun from(d: Documentable): SymbolAnchorHint? = d.name?.let { SymbolAnchorHint(it) }
    }
}