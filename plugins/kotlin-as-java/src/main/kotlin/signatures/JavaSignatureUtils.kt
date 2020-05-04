package org.jetbrains.dokka.kotlinAsJava.signatures
import javaslang.Tuple2
import org.jetbrains.dokka.base.signatures.All
import org.jetbrains.dokka.base.signatures.JvmSingatureUtils
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.ExtraModifiers.Companion.javaOnlyModifiers
import org.jetbrains.dokka.model.properties.WithExtraProperties

object JavaSignatureUtils : JvmSingatureUtils {

    val ignoredAnnotations = setOf(
        Annotations.Annotation(DRI("kotlin.jvm", "Transient"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "Volatile"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "Transitive"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "Strictfp"), emptyMap()),
        Annotations.Annotation(DRI("kotlin.jvm", "JvmStatic"), emptyMap())
    )

    private val strategy = All
    private val listBrackets = Tuple2('{', '}')
    private val classExtension = ".class"

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: Documentable) =
        annotationsBlockWithIgnored(d, ignoredAnnotations, strategy, listBrackets, classExtension)

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: Documentable) =
        annotationsInlineWithIgnored(d, ignoredAnnotations, strategy, listBrackets, classExtension)

    override fun <T : Documentable> WithExtraProperties<T>.modifiers() =
        modifiersWithFilter(javaOnlyModifiers)

}