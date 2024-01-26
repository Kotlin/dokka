/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.signatures.JvmSignatureUtils
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.javadoc.translators.documentables.JavadocPageContentBuilder
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureUtils
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger

public class JavadocSignatureProvider(
    ctcc: CommentsToContentConverter,
    logger: DokkaLogger
) : SignatureProvider, JvmSignatureUtils by JavaSignatureUtils {

    public constructor(context: DokkaContext) : this(
        context.plugin<DokkaBase>().querySingle { commentsToContentConverter },
        context.logger
    )

    private val contentBuilder = JavadocPageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default)

    private val ignoredModifiers =
        setOf(KotlinModifier.Open, JavaModifier.Empty, KotlinModifier.Empty, KotlinModifier.Sealed)

    override fun signature(documentable: Documentable): List<ContentNode> = when (documentable) {
        is DFunction -> signature(documentable)
        is DProperty -> signature(documentable)
        is DClasslike -> signature(documentable)
        is DEnumEntry -> signature(documentable)
        is DTypeParameter -> signature(documentable)
        is DParameter -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(c: DClasslike): List<ContentNode> =
        javadocSignature(c) {
            annotations {
                annotationsBlock(c)
            }
            modifiers {
                text(c.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")

                if (c is DClass) {
                    text(c.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                    text(c.modifiers()[it]?.toSignatureString() ?: "")
                }

                when (c) {
                    is DClass -> text("class")
                    is DInterface -> text("interface")
                    is DEnum -> text("enum")
                    is DObject -> text("class")
                    is DAnnotation -> text("@interface")
                }
            }
            signatureWithoutModifiers {
                link(c.dri.classNames!!, c.dri)
                if (c is WithGenerics) {
                    list(c.generics, prefix = "<", suffix = ">") {
                        +buildSignature(it)
                    }
                }
            }
            supertypes {
                if (c is WithSupertypes) {
                    c.supertypes.map { (p, dris) ->
                        val (classes, interfaces) = dris.partition { it.kind == JavaClassKindTypes.CLASS }
                        list(classes, prefix = "extends ", sourceSets = setOf(p)) {
                            link(it.typeConstructor.dri.sureClassNames, it.typeConstructor.dri, sourceSets = setOf(p))
                            list(it.typeConstructor.projections, prefix = "<", suffix = ">", sourceSets = setOf(p)) {
                                signatureForProjection(it)
                            }
                        }
                        list(interfaces, prefix = " implements ", sourceSets = setOf(p)){
                            link(it.typeConstructor.dri.sureClassNames, it.typeConstructor.dri, sourceSets = setOf(p))
                            list(it.typeConstructor.projections, prefix = "<", suffix = ">", sourceSets = setOf(p)) {
                                signatureForProjection(it)
                            }
                        }
                    }
                }
            }
        }

    private fun signature(f: DFunction): List<ContentNode> =
        javadocSignature(f) { sourceSet ->
            annotations {
                annotationsBlock(f)
            }
            modifiers {
                text(f.modifier[sourceSet]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                text(f.modifiers()[sourceSet]?.toSignatureString() ?: "")
                val usedGenerics = if (f.isConstructor) f.generics.filter { f uses it } else f.generics
                list(usedGenerics, prefix = "<", suffix = "> ") {
                    +buildSignature(it)
                }
                signatureForProjection(f.type)
            }
            signatureWithoutModifiers {
                link(f.name, f.dri)
                text("(")
                list(f.parameters) {
                    annotationsInline(it)
                    text(it.modifiers()[sourceSet]?.toSignatureString().orEmpty())
                    signatureForProjection(it.type)
                    text(Typography.nbsp.toString())
                    text(it.name.orEmpty())
                }
                text(")")
            }
        }

    private fun signature(p: DProperty): List<ContentNode> =
        javadocSignature(p) {
            annotations {
                annotationsBlock(p)
            }
            modifiers {
                text(p.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")
                text(p.modifier[it]?.name + " ")
                text(p.modifiers()[it]?.toSignatureString() ?: "")
                signatureForProjection(p.type)
            }
            signatureWithoutModifiers {
                link(p.name, p.dri)
            }
        }

    private fun signature(e: DEnumEntry): List<ContentNode> =
        javadocSignature(e) {
            annotations {
                annotationsBlock(e)
            }
            modifiers {
                text(e.modifiers()[it]?.toSignatureString() ?: "")
            }
            signatureWithoutModifiers {
                link(e.name, e.dri)
            }
        }

    private fun signature(t: DTypeParameter): List<ContentNode> =
        javadocSignature(t) {
            annotations {
                annotationsBlock(t)
            }
            signatureWithoutModifiers {
                text(t.name)
            }
            supertypes {
                list(t.bounds, prefix = "extends ") {
                    signatureForProjection(it)
                }
            }
        }

    private fun signature(p: DParameter): List<ContentNode> =
        javadocSignature(p) {
            modifiers {
                signatureForProjection(p.type)
            }
            signatureWithoutModifiers {
                link(p.name.orEmpty(), p.dri)
            }
        }

    private fun javadocSignature(
        d: Documentable,
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        block: JavadocPageContentBuilder.JavadocContentBuilder.(DokkaConfiguration.DokkaSourceSet) -> Unit
    ): List<ContentNode> =
        d.sourceSets.map { sourceSet ->
            contentBuilder.contentFor(d, ContentKind.Main) {
                with(contentBuilder) {
                    javadocGroup(d.dri, d.sourceSets, extra) {
                        block(sourceSet)
                    }
                }
            }
        }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit = when (p) {
        is TypeParameter -> link(p.name, p.dri)
        is TypeConstructor -> group {
            link(p.dri.classNames.orEmpty(), p.dri)
            list(p.projections, prefix = "<", suffix = ">") {
                signatureForProjection(it)
            }
        }
        is Variance<*> -> group {
            text("$p ".takeIf { it.isNotBlank() } ?: "")
            signatureForProjection(p.inner)
        }
        is Star -> text("?")
        is Nullable -> signatureForProjection(p.inner)
        is DefinitelyNonNullable -> signatureForProjection(p.inner)
        is JavaObject, is Dynamic -> link("Object", DRI("java.lang", "Object"))
        is Void -> text("void")
        is PrimitiveJavaType -> text(p.name)
        is UnresolvedBound -> text(p.name)
        is TypeAliased -> signatureForProjection(p.inner)
    }

    private fun DRI.fqName(): String = "${packageName.orEmpty()}.${classNames.orEmpty()}"
}
