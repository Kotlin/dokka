package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExtraModifiers
import org.jetbrains.dokka.model.properties.WithExtraProperties

object KotlinSignatureUtils : JvmSignatureUtils {

    private val strategy = OnlyOnce
    private val listBrackets = Pair('[', ']')
    private val classExtension = "::class"

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: Documentable) =
        annotationsBlockWithIgnored(d, emptySet(), strategy, listBrackets, classExtension)

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: Documentable) =
        annotationsInlineWithIgnored(d, emptySet(), strategy, listBrackets, classExtension)

    override fun <T : Documentable> WithExtraProperties<T>.modifiers() =
        extra[AdditionalModifiers]?.content?.entries?.map {
            it.key to it.value.filterIsInstance<ExtraModifiers.KotlinOnlyModifiers>().toSet()
        }?.toMap() ?: emptyMap()
}