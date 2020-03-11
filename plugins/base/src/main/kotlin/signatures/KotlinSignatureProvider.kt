package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfUnit
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Annotation
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider {
    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default, KotlinVisibility.Public)

    override fun signature(documentable: Documentable): ContentNode = when (documentable) {
        is Function -> signature(documentable)
        is Classlike -> signature(documentable)
        is TypeParameter -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(c: Classlike) = contentBuilder.contentFor(c, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        platformText(c.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }
        if (c is Class) {
            text(c.modifier.name + " ")
        }
        when (c) {
            is Class -> text("class ")
            is Interface -> text("interface ")
            is Enum -> text("enum ")
            is Object -> text("object ")
            is Annotation -> text("annotation class ")
        }
        link(c.name!!, c.dri)
        if (c is WithSupertypes) {
            c.supertypes.map { (p, dris) ->
                list(dris, prefix = " : ", platformData = setOf(p)) {
                    link(it.sureClassNames, it, platformData = setOf(p))
                }
            }
        }
    }

    private fun signature(f: Function) = contentBuilder.contentFor(f, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        platformText(f.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }
        text(f.modifier.name + " ")
        text("fun ")
        list(f.generics, prefix = "<", suffix = "> ") {
            +buildSignature(it)
        }
        f.receiver?.also {
            signatureForProjection(it.type)
            text(".")
        }
        link(f.name, f.dri)
        text("(")
        list(f.parameters) {
            text(it.name!!)
            text(": ")
            signatureForProjection(it.type)
        }
        text(")")
        val returnType = f.type
        if (!f.isConstructor && returnType is TypeConstructor && returnType.dri != DriOfUnit) {
            text(": ")
            signatureForProjection(returnType)
        }
    }

    private fun signature(t: TypeParameter) = contentBuilder.contentFor(t, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        link(t.name, t.dri)
        list(t.bounds, prefix = " : ") {
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
            text(p.kind.toString() + " ")
            signatureForProjection(p.inner)
        }

        is Star -> text("*")

        is Nullable -> group {
            signatureForProjection(p.inner)
            text("?")
        }

        is PrimitiveJavaType -> signatureForProjection(p.translateToKotlin())
    }
}

private fun PrimitiveJavaType.translateToKotlin() = TypeConstructor(
    dri = DRI("kotlin", name.capitalize()),
    projections = emptyList()
)
