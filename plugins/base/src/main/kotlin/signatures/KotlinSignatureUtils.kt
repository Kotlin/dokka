package org.jetbrains.dokka.base.signatures

import javaslang.Tuple2
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExtraModifiers
import org.jetbrains.dokka.model.ExtraModifiers.Companion.kotlinOnlyModifiers
import org.jetbrains.dokka.model.properties.WithExtraProperties

object KotlinSignatureUtils : JvmSingatureUtils {

    private val strategy = OnlyOnce
    private val listBrackets = Tuple2('[', ']')
    private val classExtension = "::class"

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: Documentable) =
        annotationsBlockWithIgnored(d, emptySet(), strategy, listBrackets, classExtension)

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: Documentable) =
        annotationsInlineWithIgnored(d, emptySet(), strategy, listBrackets, classExtension)

    override fun <T : Documentable> WithExtraProperties<T>.modifiers() =
        modifiersWithFilter(kotlinOnlyModifiers)
}