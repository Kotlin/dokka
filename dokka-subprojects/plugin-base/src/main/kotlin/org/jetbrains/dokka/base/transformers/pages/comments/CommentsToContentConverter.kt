/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.comments

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.DCI
import org.jetbrains.dokka.pages.Style

public interface CommentsToContentConverter {
    public fun buildContent(
        docTag: DocTag,
        dci: DCI,
        sourceSets: Set<DokkaSourceSet>,
        styles: Set<Style> = emptySet(),
        extras: PropertyContainer<ContentNode> = PropertyContainer.empty()
    ): List<ContentNode>
}
