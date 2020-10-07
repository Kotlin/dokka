package org.jetbrains.dokka.base.resolvers.anchors

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.Kind

data class SymbolAnchorHint(val anchorName: String, val contentKind: Kind) : ExtraProperty<ContentNode> {
    override val key: ExtraProperty.Key<ContentNode, SymbolAnchorHint> = SymbolAnchorHint

    companion object : ExtraProperty.Key<ContentNode, SymbolAnchorHint> {
        fun from(d: Documentable, contentKind: Kind): SymbolAnchorHint? =
            d.name?.let { SymbolAnchorHint(it, contentKind) }
    }
}
