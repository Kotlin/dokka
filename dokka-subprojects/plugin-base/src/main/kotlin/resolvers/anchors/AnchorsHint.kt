/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.anchors

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.Kind

public data class SymbolAnchorHint(val anchorName: String, val contentKind: Kind) : ExtraProperty<ContentNode> {
    override val key: ExtraProperty.Key<ContentNode, SymbolAnchorHint> = SymbolAnchorHint

    public companion object : ExtraProperty.Key<ContentNode, SymbolAnchorHint> {
        public fun from(d: Documentable, contentKind: Kind): SymbolAnchorHint? =
            d.name?.let { SymbolAnchorHint(it, contentKind) }
    }
}
