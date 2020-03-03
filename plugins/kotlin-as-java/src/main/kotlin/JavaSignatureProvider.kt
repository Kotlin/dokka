package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
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

    override fun signature(documentable: Documentable): List<ContentNode> = when (documentable) {
        is Function -> signature(documentable)
        is Classlike -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(f: Function) = f.platformData.map { signature(f, it) }.distinct()

    private fun signature(c: Classlike) = c.platformData.map { signature(c, it) }.distinct()

    private fun signature(c: Classlike, platform: PlatformData) = contentBuilder.contentFor(c, ContentKind.Symbol) {
        if (c is Class) {
            text(c.modifier.takeIf { it != WithAbstraction.Modifier.Empty }?.toString()?.toLowerCase().orEmpty())
        }
        when (c) {
            is Class -> text(" class ")
            is Interface -> text(" interface ")
            is Enum -> text(" enum ")
            is Object -> text(" class ")
            is Annotation -> text(" @interface ")
        }
        text(c.name!!)
        if (c is WithGenerics) {
            val generics = c.generics.filterOnPlatform(platform)
            if (generics.isNotEmpty()) {
                text("<")
                list(generics) {
                    +this@JavaSignatureProvider.signature(it)
                }
                text(">")
            }
        }
        if (c is WithSupertypes && c.supertypes.containsKey(platform)) {
            list(c.supertypes.getValue(platform), prefix = " extends ") {
                link(it.sureClassNames, it)
            }
        }
    }

    private fun signature(f: Function, platform: PlatformData) = contentBuilder.contentFor(f, ContentKind.Symbol) {
        text(f.modifier.takeIf { it != WithAbstraction.Modifier.Empty }?.toString()?.toLowerCase().orEmpty() + " ")
        val returnType = f.type
        if (!f.isConstructor && returnType.constructorFqName != Unit::class.qualifiedName) {
            type(returnType)
        }
        text("  ")
        link(f.name, f.dri)
        val generics = f.generics.filterOnPlatform(platform)
        if (generics.isNotEmpty()) {
            text("<")
            generics.forEach {
                this@JavaSignatureProvider.signature(it)
            }
            text(">")
        }
        text("(")
        list(f.parameters.filterOnPlatform(platform)) {
            type(it.type)
            text(" ")
            link(it.name!!, it.dri)
        }
        text(")")
    }

    private fun signature(t: TypeParameter) = contentBuilder.contentFor(t, ContentKind.Symbol) {
        text(t.name.substringAfterLast("."))
        if (t.bounds.isNotEmpty()) {
            text(" extends ")
            t.bounds.forEach {
                +signature(it, t.dri, t.platformData)
            }
        }
    }

    private fun signature(p: Projection, dri: DRI, platforms: List<PlatformData>): List<ContentNode> = when (p) {
        is OtherParameter -> contentBuilder.contentFor(dri, platforms.toSet()) { text(p.name) }.children

        is TypeConstructor -> contentBuilder.contentFor(dri, platforms.toSet()) {
            link(p.dri.classNames.orEmpty(), p.dri)
            list(p.projections, prefix = "<", suffix = ">") {
                +signature(it, dri, platforms)
            }
        }.children

        is Variance -> contentBuilder.contentFor(dri, platforms.toSet()) {
            text(p.kind.toString() + " ")
        }.children + signature(p.inner, dri, platforms)

        is Star -> contentBuilder.contentFor(dri, platforms.toSet()) { text("?") }.children

        is Nullable -> signature(p.inner, dri, platforms) + contentBuilder.contentFor(
            dri,
            platforms.toSet()
        ) { text("?") }.children
    }

    private fun <T : Documentable> Collection<T>.filterOnPlatform(platformData: PlatformData) =
        this.filter { it.platformData.contains(platformData) }
}