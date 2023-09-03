/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava.signatures

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.signatures.JvmSignatureUtils
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.pages.TokenStyle
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.text.Typography.nbsp

public class JavaSignatureProvider internal constructor(
    ctcc: CommentsToContentConverter,
    logger: DokkaLogger
) : SignatureProvider, JvmSignatureUtils by JavaSignatureUtils {

    public constructor(context: DokkaContext) : this(
        context.plugin<DokkaBase>().querySingle { commentsToContentConverter },
        context.logger
    )

    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default)

    private val ignoredModifiers =
        setOf(KotlinModifier.Open, JavaModifier.Empty, KotlinModifier.Empty, KotlinModifier.Sealed)

    override fun signature(documentable: Documentable): List<ContentNode> = when (documentable) {
        is DFunction -> signature(documentable)
        is DProperty -> signature(documentable)
        is DClasslike -> signature(documentable)
        is DEnumEntry -> signature(documentable)
        is DTypeParameter -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(e: DEnumEntry) =
        e.sourceSets.map {
            contentBuilder.contentFor(
                e,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace),
                sourceSets = setOf(it)
            ) {
                link(e.name, e.dri, styles = mainStyles + e.stylesIfDeprecated(it))
            }
        }

    private fun signature(c: DClasslike) =
        c.sourceSets.map { sourceSet ->
            @Suppress("UNCHECKED_CAST")
            val deprecationStyles = (c as? WithExtraProperties<out Documentable>)
                ?.stylesIfDeprecated(sourceSet)
                ?: emptySet()

            contentBuilder.contentFor(
                c,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace),
                sourceSets = setOf(sourceSet)
            ) {
                annotationsBlock(c)
                c.visibility[sourceSet]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ")?.let { keyword(it) }

                if (c is DClass) {
                    c.modifier[sourceSet]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ")?.let { keyword(it) }
                    c.modifiers()[sourceSet]?.toSignatureString()?.let { keyword(it) }
                }

                when (c) {
                    is DClass -> keyword("class ")
                    is DInterface -> keyword("interface ")
                    is DEnum -> keyword("enum ")
                    is DObject -> keyword("class ")
                    is DAnnotation -> keyword("@interface ")
                }
                link(c.name!!, c.dri, styles = mainStyles + deprecationStyles)
                if (c is WithGenerics) {
                    list(c.generics, prefix = "<", suffix = ">",
                        separatorStyles = mainStyles + TokenStyle.Punctuation,
                        surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                        +buildSignature(it)
                    }
                }
                if (c is WithSupertypes) {
                    c.supertypes.map { (p, dris) ->
                        val (classes, interfaces) = dris.partition { it.kind == JavaClassKindTypes.CLASS }
                        list(classes, prefix = " extends ", sourceSets = setOf(p),
                            separatorStyles = mainStyles + TokenStyle.Punctuation,
                            surroundingCharactersStyle = mainStyles + TokenStyle.Keyword) {
                            signatureForProjection(it.typeConstructor)
                        }
                        list(interfaces, prefix = " implements ", sourceSets = setOf(p),
                            separatorStyles = mainStyles + TokenStyle.Punctuation,
                            surroundingCharactersStyle = mainStyles + TokenStyle.Keyword) {
                            signatureForProjection(it.typeConstructor)
                        }
                    }
                }
            }
        }

    private fun signature(p: DProperty) =
        p.sourceSets.map {
            contentBuilder.contentFor(
                p,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace, TextStyle.Block),
                sourceSets = setOf(it)
            ) {
                annotationsBlock(p)
                p.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                p.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.let { keyword("$it ") }
                p.modifiers()[it]?.toSignatureString()?.let { keyword(it) }
                signatureForProjection(p.type)
                text(nbsp.toString())
                link(p.name, p.dri, styles = mainStyles + p.stylesIfDeprecated(it))
            }
        }

    private fun signature(f: DFunction) =
        f.sourceSets.map { sourceSet ->
            contentBuilder.contentFor(
                f,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace, TextStyle.Block),
                sourceSets = setOf(sourceSet)
            ) {
                annotationsBlock(f)
                f.visibility[sourceSet]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                f.modifier[sourceSet]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ")?.let { keyword(it) }
                f.modifiers()[sourceSet]?.toSignatureString()?.let { keyword(it) }
                val returnType = f.type
                signatureForProjection(returnType)
                text(nbsp.toString())
                link(f.name, f.dri, styles = mainStyles + TokenStyle.Function + f.stylesIfDeprecated(sourceSet))
                val usedGenerics = if (f.isConstructor) f.generics.filter { f uses it } else f.generics
                list(usedGenerics, prefix = "<", suffix = ">",
                    separatorStyles = mainStyles + TokenStyle.Punctuation,
                    surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                    +buildSignature(it)
                }
                punctuation("(")
                if (f.parameters.isNotEmpty()) {
                    parametersBlock(f) {
                        annotationsInline(it)
                        text(it.modifiers()[sourceSet]?.toSignatureString() ?: "", styles = mainStyles + TokenStyle.Keyword)
                        signatureForProjection(it.type)
                        text(nbsp.toString())
                        text(it.name!!)
                    }
                }
                punctuation(")")
            }
        }

    private fun signature(t: DTypeParameter) =
        t.sourceSets.map {
            contentBuilder.contentFor(t, sourceSets = setOf(it)) {
                annotationsInline(t)
                text(t.name.substringAfterLast("."), styles = mainStyles + t.stylesIfDeprecated(it))
                list(
                    elements = t.bounds,
                    prefix = " extends ",
                    separatorStyles = mainStyles + TokenStyle.Punctuation,
                    surroundingCharactersStyle = mainStyles + TokenStyle.Keyword
                ) {
                    signatureForProjection(it)
                }
            }

        }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit = when (p) {
        is TypeParameter -> {
            annotationsInline(p)
            link(p.name, p.dri)
        }

        is TypeConstructor -> group(styles = emptySet()) {
            annotationsInline(p)
            link(p.dri.classNames.orEmpty(), p.dri)
            list(p.projections, prefix = "<", suffix = ">",
                separatorStyles = mainStyles + TokenStyle.Punctuation,
                surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                signatureForProjection(it)
            }
        }

        is Variance<*> -> group(styles = emptySet()) {
            val variance = when(p) {
                is Covariance<*> -> "? extends "
                is Contravariance<*> -> "? super "
                is Invariance<*> -> ""
            }
            keyword(variance)
            signatureForProjection(p.inner)
        }

        is Star -> operator("?")

        is Nullable -> signatureForProjection(p.inner)
        is DefinitelyNonNullable -> signatureForProjection(p.inner)

        is JavaObject, is Dynamic -> link("Object", DRI("java.lang", "Object"))
        is Void -> text("void")
        is PrimitiveJavaType -> text(p.name)
        is UnresolvedBound -> text(p.name)
        is TypeAliased -> signatureForProjection(p.inner)
    }
}
