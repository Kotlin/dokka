package org.jetbrains.dokka.pages.anchors

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

// TODO IMPORTANT: https://github.com/Kotlin/dokka/issues/1054
// TODO: Probably it should be moved to some plugin but it is hard to decide which one
object SymbolAnchorHint: ExtraProperty<ContentNode>, ExtraProperty.Key<ContentNode, SymbolAnchorHint> {
    override val key = this
}