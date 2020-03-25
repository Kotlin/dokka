package org.jetbrains.dokka.kotlinAsJava.signatures

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.utilities.DokkaLogger

class JavaSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider {
    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default, KotlinVisibility.Public)
    private val ignoredModifiers =
        setOf(KotlinModifier.Open, JavaModifier.Empty, KotlinModifier.Empty, KotlinModifier.Sealed)


    override fun signature(documentable: Documentable): ContentNode = when (documentable) {
        is DFunction -> signature(documentable)
        is DProperty -> signature(documentable)
        is DClasslike -> signature(documentable)
        is DEnumEntry -> signature(documentable)
        is DTypeParameter -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(e: DEnumEntry)= contentBuilder.contentFor(e, ContentKind.Symbol, setOf(TextStyle.Monospace))

    private fun signature(c: DClasslike) = contentBuilder.contentFor(c, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        platformText(c.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }

        if (c is DClass) {
            platformText(c.modifier){ it.takeIf{it !in ignoredModifiers}?.name.orEmpty() + " "}
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
                list(dris, prefix = " extends ", platformData = setOf(p)) {
                    link(it.sureClassNames, it, platformData = setOf(p))
                }
            }
        }
    }

    private fun signature(p: DProperty) = contentBuilder.contentFor(p, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        signatureForProjection(p.type)
    }

    private fun signature(f: DFunction) = contentBuilder.contentFor(f, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        platformText(f.modifier){ it.takeIf{it !in ignoredModifiers}?.name.orEmpty() + " "}
        val returnType = f.type
        signatureForProjection(returnType)
        text("  ")
        link(f.name, f.dri)
        list(f.generics, prefix = "<", suffix = ">") {
            +buildSignature(it)
        }
        text("(")
        list(f.parameters) {
            signatureForProjection(it.type)
            text(" ")
            link(it.name!!, it.dri)
        }
        text(")")
    }

    private fun signature(t: DTypeParameter) = contentBuilder.contentFor(t) {
        text(t.name.substringAfterLast("."))
        list(t.bounds, prefix = " extends ") {
            signatureForProjection(it)
        }
    }


    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit = when (p) {
        is OtherParameter -> text(p.name)

        is TypeConstructor -> group {
            link(p.dri.classNames.orEmpty(), p.dri)
            list(p.projections, prefix = "<", suffix = ">") {
                signatureForProjection(it)
            }
        }

        is Variance -> group {
            text(p.kind.toString() + " ") // TODO: "super" && "extends"
            signatureForProjection(p.inner)
        }

        is Star -> text("?")

        is Nullable -> signatureForProjection(p.inner)

        is JavaObject -> link("Object", DRI("java.lang", "Object"))
        is Void -> text("void")
        is PrimitiveJavaType -> text(p.name)
    }
}