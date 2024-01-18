/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.tags

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinVersion
import org.jetbrains.dokka.base.translators.documentables.KDOC_TAG_HEADER_LEVEL
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.pages.TextStyle

public object SinceKotlinTagContentProvider : CustomTagContentProvider {

    override fun isApplicable(customTag: CustomTagWrapper): Boolean =
        customTag.name == SinceKotlinVersion.SINCE_KOTLIN_TAG_NAME

    override fun DocumentableContentBuilder.contentForDescription(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        group(sourceSets = setOf(sourceSet), styles = emptySet()) {
            header(KDOC_TAG_HEADER_LEVEL, customTag.name)
            comment(customTag.root)
        }
    }

    override fun DocumentableContentBuilder.contentForBrief(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        group(sourceSets = setOf(sourceSet), styles = setOf(TextStyle.InlineComment)) {
            text(customTag.name + " ", styles = setOf(TextStyle.Bold))
            comment(customTag.root, styles = emptySet())
        }
    }
}
