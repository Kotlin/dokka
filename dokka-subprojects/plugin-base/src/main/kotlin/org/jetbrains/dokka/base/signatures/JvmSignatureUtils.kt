/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.drisOfAllNestedBounds
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*

public interface JvmSignatureUtils {

    public fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: AnnotationTarget)

    public fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: AnnotationTarget)

    public fun <T : Documentable> WithExtraProperties<T>.modifiers(): SourceSetDependent<Set<ExtraModifiers>>

    public fun Collection<ExtraModifiers>.toSignatureString(): String =
        joinToString("") { it.name.toLowerCase() + " " }

    @Suppress("UNCHECKED_CAST")
    public fun Documentable.annotations(): Map<DokkaSourceSet, List<Annotations.Annotation>> {
        return (this as? WithExtraProperties<Documentable>)?.annotations() ?: emptyMap()
    }

    public fun <T : AnnotationTarget> WithExtraProperties<T>.annotations(): SourceSetDependent<List<Annotations.Annotation>> =
        extra[Annotations]?.directAnnotations ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Iterable<*>> SourceSetDependent<T>.plus(other: SourceSetDependent<T>): SourceSetDependent<T> {
        return LinkedHashMap(this).apply {
            for ((k, v) in other) {
                put(k, get(k).let { if (it != null) (it + v) as T else v })
            }
        }
    }

    public fun DProperty.annotations(): SourceSetDependent<List<Annotations.Annotation>> {
        return (extra[Annotations]?.directAnnotations ?: emptyMap()) +
                (getter?.annotations() ?: emptyMap()).mapValues { it.value.map { it.copy( scope = Annotations.AnnotationScope.GETTER) } } +
                (setter?.annotations() ?: emptyMap()).mapValues { it.value.map { it.copy( scope = Annotations.AnnotationScope.SETTER) } }
    }

    private fun PageContentBuilder.DocumentableContentBuilder.annotations(
        d: AnnotationTarget,
        ignored: Set<Annotations.Annotation>,
        styles: Set<Style>,
        operation: PageContentBuilder.DocumentableContentBuilder.(Annotations.Annotation) -> Unit
    ): Unit = when (d) {
        is DFunction -> d.annotations()
        is DProperty -> d.annotations()
        is DClass -> d.annotations()
        is DInterface -> d.annotations()
        is DObject -> d.annotations()
        is DEnum -> d.annotations()
        is DAnnotation -> d.annotations()
        is DTypeParameter -> d.annotations()
        is DEnumEntry -> d.annotations()
        is DTypeAlias -> d.annotations()
        is DParameter -> d.annotations()
        is TypeParameter -> d.annotations()
        is GenericTypeConstructor -> d.annotations()
        is FunctionalTypeConstructor -> d.annotations()
        is JavaObject -> d.annotations()
        else -> null
    }?.let {
        it.entries.forEach {
            it.value.filter { it !in ignored && it.mustBeDocumented }.takeIf { it.isNotEmpty() }?.let { annotations ->
                group(sourceSets = setOf(it.key), styles = styles, kind = ContentKind.Annotations) {
                    annotations.forEach {
                        operation(it)
                    }
                }
            }
        }
    } ?: Unit

    public fun PageContentBuilder.DocumentableContentBuilder.toSignatureString(
        a: Annotations.Annotation,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ) {

        when (renderAtStrategy) {
            is All, is OnlyOnce -> {
                when(a.scope) {
                    Annotations.AnnotationScope.GETTER -> text("@get:", styles = mainStyles + TokenStyle.Annotation)
                    Annotations.AnnotationScope.SETTER -> text("@set:", styles = mainStyles + TokenStyle.Annotation)
                    else -> text("@", styles = mainStyles + TokenStyle.Annotation)
                }
                link(a.dri.classNames!!, a.dri, styles = mainStyles + TokenStyle.Annotation)
            }
            is Never -> link(a.dri.classNames!!, a.dri)
        }
        val isNoWrappedBrackets = a.params.entries.isEmpty() && renderAtStrategy is OnlyOnce
        listParams(
            a.params.entries,
            if (isNoWrappedBrackets) null else Pair('(', ')')
        ) {
            text(it.key)
            text(" = ", styles = mainStyles + TokenStyle.Operator)
            when (renderAtStrategy) {
                is All -> All
                is Never, is OnlyOnce -> Never
            }.let { strategy ->
                valueToSignature(it.value, strategy, listBrackets, classExtension)
            }
        }
    }

    private fun PageContentBuilder.DocumentableContentBuilder.valueToSignature(
        a: AnnotationParameterValue,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ): Unit = when (a) {
        is AnnotationValue -> toSignatureString(a.annotation, renderAtStrategy, listBrackets, classExtension)
        is ArrayValue -> {
            listParams(a.value, listBrackets) { valueToSignature(it, renderAtStrategy, listBrackets, classExtension) }
        }
        is EnumValue -> link(a.enumName, a.enumDri)
        is ClassValue -> link(a.className + classExtension, a.classDRI)
        is StringValue -> group(styles = setOf(TextStyle.Breakable)) { stringLiteral( "\"${a.text()}\"") }
        is BooleanValue -> group(styles = setOf(TextStyle.Breakable)) { booleanLiteral(a.value) }
        is LiteralValue -> group(styles = setOf(TextStyle.Breakable)) { constant(a.text()) }
    }

    private fun<T> PageContentBuilder.DocumentableContentBuilder.listParams(
        params: Collection<T>,
        listBrackets: Pair<Char, Char>?,
        outFn: PageContentBuilder.DocumentableContentBuilder.(T) -> Unit
    ) {
        listBrackets?.let{ punctuation(it.first.toString()) }
        params.forEachIndexed { i, it ->
            group(styles = setOf(TextStyle.BreakableAfter)) {
                this.outFn(it)
                if (i != params.size - 1) punctuation(", ")
            }
        }
        listBrackets?.let{ punctuation(it.second.toString()) }
    }

    public fun PageContentBuilder.DocumentableContentBuilder.annotationsBlockWithIgnored(
        d: AnnotationTarget,
        ignored: Set<Annotations.Annotation>,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ) {
        annotations(d, ignored, setOf(TextStyle.Block)) {
            group {
                toSignatureString(it, renderAtStrategy, listBrackets, classExtension)
            }
        }
    }

    public fun PageContentBuilder.DocumentableContentBuilder.annotationsInlineWithIgnored(
        d: AnnotationTarget,
        ignored: Set<Annotations.Annotation>,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ) {
        annotations(d, ignored, setOf(TextStyle.Span)) {
            toSignatureString(it, renderAtStrategy, listBrackets, classExtension)
            text(Typography.nbsp.toString())
        }
    }

    public fun <T : Documentable> WithExtraProperties<T>.stylesIfDeprecated(sourceSetData: DokkaSourceSet): Set<TextStyle> {
        val directAnnotations = extra[Annotations]?.directAnnotations?.get(sourceSetData) ?: emptyList()
        val hasAnyDeprecatedAnnotation =
            directAnnotations.any { it.dri == DRI("kotlin", "Deprecated") || it.dri == DRI("java.lang", "Deprecated") }

        return if (hasAnyDeprecatedAnnotation) setOf(TextStyle.Strikethrough) else emptySet()
    }

    public infix fun DFunction.uses(typeParameter: DTypeParameter): Boolean {
        val parameterDris = parameters.flatMap { listOf(it.dri) + it.type.drisOfAllNestedBounds }
        val receiverDris =
            listOfNotNull(
                receiver?.dri,
                *receiver?.type?.drisOfAllNestedBounds?.toTypedArray() ?: emptyArray()
            )
        val allDris = parameterDris + receiverDris
        return typeParameter.dri in allDris
    }

    /**
     * Builds a distinguishable [function] parameters block, so that it
     * can be processed or custom rendered down the road.
     *
     * Resulting structure:
     * ```
     * SymbolContentKind.Parameters(style = wrapped) {
     *     SymbolContentKind.Parameter(style = indented) { param, }
     *     SymbolContentKind.Parameter(style = indented) { param, }
     *     SymbolContentKind.Parameter(style = indented) { param }
     * }
     * ```
     * Wrapping and indentation of parameters is applied conditionally, see [shouldWrapParams]
     */
    public fun PageContentBuilder.DocumentableContentBuilder.parametersBlock(
        function: DFunction,
        paramBuilder: PageContentBuilder.DocumentableContentBuilder.(DParameter) -> Unit
    ) {
        group(kind = SymbolContentKind.Parameters, styles = emptySet()) {
            function.parameters.dropLast(1).forEach {
                group(kind = SymbolContentKind.Parameter) {
                    paramBuilder(it)
                    punctuation(", ")
                }
            }
            group(kind = SymbolContentKind.Parameter) {
                paramBuilder(function.parameters.last())
            }
        }
    }
}

public sealed class AtStrategy
public object All : AtStrategy()
public object OnlyOnce : AtStrategy()
public object Never : AtStrategy()
