package org.jetbrains.dokka.kotlinAsJava.signatures

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.signatures.JvmSignatureUtils
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.text.Typography.nbsp

class JavaSignatureProvider internal constructor(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider,
    JvmSignatureUtils by JavaSignatureUtils {
    constructor(context: DokkaContext) : this(
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
                setOf(TextStyle.Monospace) + e.stylesIfDeprecated(it),
                sourceSets = setOf(it)
            ) {
                link(e.name, e.dri)
            }
        }

    private fun signature(c: DClasslike) =
        c.sourceSets.map {
            contentBuilder.contentFor(
                c,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace) + ((c as? WithExtraProperties<out Documentable>)?.stylesIfDeprecated(it)
                    ?: emptySet()),
                sourceSets = setOf(it)
            ) {
                text(c.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")

                if (c is DClass) {
                    text(c.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                    text(c.modifiers()[it]?.toSignatureString() ?: "")
                }

                when (c) {
                    is DClass -> text("class ")
                    is DInterface -> text("interface ")
                    is DEnum -> text("enum ")
                    is DObject -> text("class ")
                    is DAnnotation -> text("@interface ")
                }
                link(c.name!!, c.dri)
                if (c is WithGenerics) {
                    list(c.generics, prefix = "<", suffix = ">") {
                        +buildSignature(it)
                    }
                }
                if (c is WithSupertypes) {
                    c.supertypes.map { (p, dris) ->
                        val (classes, interfaces) = dris.partition { it.kind == JavaClassKindTypes.CLASS }
                        list(classes, prefix = " extends ", sourceSets = setOf(p)) {
                            signatureForProjection(it.typeConstructor)
                        }
                        list(interfaces, prefix = " implements ", sourceSets = setOf(p)) {
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
                setOf(TextStyle.Monospace, TextStyle.Block) + p.stylesIfDeprecated(it),
                sourceSets = setOf(it)
            ) {
                annotationsBlock(p)
                text(p.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")
                text(p.modifier[it]?.name + " ")
                text(p.modifiers()[it]?.toSignatureString() ?: "")
                signatureForProjection(p.type)
                text(nbsp.toString())
                link(p.name, p.dri)
            }
        }

    private fun signature(f: DFunction) =
        f.sourceSets.map { sourceSet ->
            contentBuilder.contentFor(
                f,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace, TextStyle.Block) + f.stylesIfDeprecated(sourceSet),
                sourceSets = setOf(sourceSet)
            ) {
                annotationsBlock(f)
                text(f.modifier[sourceSet]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                text(f.modifiers()[sourceSet]?.toSignatureString() ?: "")
                val returnType = f.type
                signatureForProjection(returnType)
                text(nbsp.toString())
                link(f.name, f.dri)
                val usedGenerics = if (f.isConstructor) f.generics.filter { f uses it } else f.generics
                list(usedGenerics, prefix = "<", suffix = ">") {
                    +buildSignature(it)
                }
                text("(")
                list(f.parameters) {
                    annotationsInline(it)
                    text(it.modifiers()[it]?.toSignatureString() ?: "")
                    signatureForProjection(it.type)
                    text(nbsp.toString())
                    text(it.name!!)
                }
                text(")")
            }
        }

    private fun signature(t: DTypeParameter) =
        t.sourceSets.map {
            contentBuilder.contentFor(t, styles = t.stylesIfDeprecated(it), sourceSets = setOf(it)) {
                text(t.name.substringAfterLast("."))
                list(t.bounds, prefix = " extends ") {
                    signatureForProjection(it)
                }
            }

        }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit = when (p) {
        is TypeParameter -> link(p.name, p.dri)

        is TypeConstructor -> group(styles = emptySet()) {
            link(p.dri.classNames.orEmpty(), p.dri)
            list(p.projections, prefix = "<", suffix = ">") {
                signatureForProjection(it)
            }
        }

        is Variance<*> -> group(styles = emptySet()) {
            val variance = when(p) {
                is Covariance<*> -> "? extends "
                is Contravariance<*> -> "? super "
                is Invariance<*> -> ""
            }
            text(variance)
            signatureForProjection(p.inner)
        }

        is Star -> text("?")

        is Nullable -> signatureForProjection(p.inner)

        is JavaObject, is Dynamic -> link("Object", DRI("java.lang", "Object"))
        is Void -> text("void")
        is PrimitiveJavaType -> text(p.name)
        is UnresolvedBound -> text(p.name)
        is TypeAliased -> signatureForProjection(p.inner)
    }
}
