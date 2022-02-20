package org.jetbrains.dokka.base.transformers.pages.tags

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.TextStyle

object SinceKotlinTagContentProvider : CustomTagContentProvider {

    private const val SINCE_KOTLIN_TAG_NAME = "Since Kotlin"

    override fun isApplicable(customTag: CustomTagWrapper) = customTag.name == SINCE_KOTLIN_TAG_NAME

    override fun DocumentableContentBuilder.contentForDescription(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        group(sourceSets = setOf(sourceSet), kind = ContentKind.Comment, styles = setOf(TextStyle.Block)) {
            header(4, customTag.name)
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