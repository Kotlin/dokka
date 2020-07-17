package org.jetbrains.dokka.base.resolvers.anchors

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

// TODO IMPORTANT: https://github.com/Kotlin/dokka/issues/1054
object SymbolAnchorHint: ExtraProperty<ContentNode>, ExtraProperty.Key<ContentNode, SymbolAnchorHint> {
    override val key = this
}