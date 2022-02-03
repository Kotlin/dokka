package org.jetbrains.dokka.base.transformers.pages.tags

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.TextStyle

object SinceKotlinTagContentProvider : CustomTagContentProvider {

    private val TAG_NAME = "Since Kotlin"

    override fun DocumentableContentBuilder.contentForDescription(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        sinceKotlinBlock(sourceSet, customTag)
    }

    override fun DocumentableContentBuilder.contentForBrief(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        sinceKotlinBlock(sourceSet, customTag)
    }

    private fun DocumentableContentBuilder.sinceKotlinBlock(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        if (customTag.name == TAG_NAME) {
            group(sourceSets = setOf(sourceSet), kind = ContentKind.Comment, styles = setOf(TextStyle.Block)) {
                header(4, customTag.name)
                comment(customTag.root)
            }
        }
    }
}