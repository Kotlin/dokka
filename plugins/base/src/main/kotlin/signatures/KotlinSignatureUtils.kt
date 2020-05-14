package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExtraModifiers.Companion.kotlinOnlyModifiers
import org.jetbrains.dokka.model.properties.WithExtraProperties

object KotlinSignatureUtils : JvmSingatureUtils {

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: Documentable) =
        annotationsBlockWithIgnored(d, emptySet())

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: Documentable) =
        annotationsInlineWithIgnored(d, emptySet())

    override fun <T : Documentable> WithExtraProperties<T>.modifiers() =
        modifiersWithFilter(kotlinOnlyModifiers)
}