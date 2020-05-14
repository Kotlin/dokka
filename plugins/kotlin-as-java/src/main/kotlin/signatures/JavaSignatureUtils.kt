package org.jetbrains.dokka.kotlinAsJava.signatures
import org.jetbrains.dokka.base.signatures.JvmSingatureUtils
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExtraModifiers.Companion.javaOnlyModifiers
import org.jetbrains.dokka.model.JavaModifier
import org.jetbrains.dokka.model.KotlinModifier
import org.jetbrains.dokka.model.properties.WithExtraProperties

object JavaSignatureUtils : JvmSingatureUtils {

    val ignoredAnnotations = setOf(
        Annotations.Annotation(DRI("kotlin.jvm", "Transient"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "Volatile"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "Transitive"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "Strictfp"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "JvmStatic"), emptyMap())
    )

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: Documentable) =
        annotationsBlockWithIgnored(d, ignoredAnnotations)

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: Documentable) =
        annotationsInlineWithIgnored(d, ignoredAnnotations)

    override fun <T : Documentable> WithExtraProperties<T>.modifiers() =
        modifiersWithFilter(javaOnlyModifiers)
}