package org.jetbrains.dokka.kotlinAsJava.signatures

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Annotation
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.utilities.DokkaLogger

class JavaSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider {
    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default, KotlinVisibility.Public)
    private val ignoredModifiers =
        setOf(KotlinModifier.Open, JavaModifier.Empty, KotlinModifier.Empty, KotlinModifier.Sealed)


    override fun signature(documentable: Documentable): ContentNode = when (documentable) {
        is Function -> signature(documentable)
        is Classlike -> signature(documentable)
        is TypeParameter -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(c: Classlike) = contentBuilder.contentFor(c, ContentKind.Symbol) {
        platformText(c.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }

        if (c is Class) {
            text(c.modifier.takeIf { it !in ignoredModifiers }?.name.orEmpty() + " ")
        }

        when (c) {
            is Class -> text("class ")
            is Interface -> text("interface ")
            is Enum -> text("enum ")
            is Object -> text("class ")
            is Annotation -> text("@interface ")
        }
        text(c.name!!)
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

    private fun signature(f: Function) = contentBuilder.contentFor(f, ContentKind.Symbol) {
        text(f.modifier.takeIf { it !in ignoredModifiers }?.name.orEmpty() + " ")
        val returnType = f.type
        if (!f.isConstructor && returnType.constructorFqName != Unit::class.qualifiedName) {
            type(returnType)
        }
        text("  ")
        link(f.name, f.dri)
        list(f.generics, prefix = "<", suffix = ">") {
            +buildSignature(it)
        }
        text("(")
        list(f.parameters) {
            type(it.type)
            text(" ")
            link(it.name!!, it.dri)
        }
        text(")")
    }

    private fun signature(t: TypeParameter) = contentBuilder.contentFor(t, ContentKind.Symbol) {
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
            text(p.kind.toString() + " ")
            signatureForProjection(p.inner)
        }

        is Star -> text("?")

        is Nullable -> signatureForProjection(p.inner)
    }
}