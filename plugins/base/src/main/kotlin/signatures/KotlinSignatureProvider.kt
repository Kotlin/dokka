package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider {
    private val contentBuilder = PageContentBuilder(ctcc, logger)

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
        text(c.visibility[platform]?.externalDisplayName ?: "")
        if (c is Class) {
            text(c.modifier.toString())
        }
        when (c) {
            is Class -> text(" class ")
            is Interface -> text(" interface ")
            is Enum -> text(" enum ")
            is Object -> text(" object ")
        }
        text(c.name!!)
        if (c is WithSupertypes) {
            list(c.supertypes.getValue(platform), prefix = " : ") {
                link(it.sureClassNames, it)
            }
        }
    }

    private fun signature(f: Function, platform: PlatformData) = contentBuilder.contentFor(f, ContentKind.Symbol) {
        text(f.visibility[platform]?.externalDisplayName ?: "")
        text(f.modifier.toString())
        text(" fun ")
        f.receiver?.also {
            type(it.type)
            text(".")
        }
        link(f.name, f.dri)
        val generics = f.generics.filterOnPlatform(platform)
        if (generics.isNotEmpty()) {
            text("<")
            generics.forEach {
                signature(it)
            }
            text(">")
        }
        text("(")
        list(f.parameters.filterOnPlatform(platform)) {
            link(it.name!!, it.dri)
            text(": ")
            type(it.type)
        }
        text(")")
        val returnType = f.type
        if (!f.isConstructor && returnType.constructorFqName != Unit::class.qualifiedName) {
            text(": ")

            type(returnType)
        }
    }

    private fun signature(t: TypeParameter) = contentBuilder.contentFor(t, ContentKind.Symbol) {
        link(t.name, t.dri)
        if (t.bounds.isNotEmpty()) {
            text("<")
            t.bounds.forEach {
                signature(it, t.dri, t.platformData)
            }
            text(">")
        }
    }

    private fun signature(p: Projection, dri: DRI, platforms: List<PlatformData>): List<ContentNode> = when (p) {
        is OtherParameter -> contentBuilder.contentFor(dri, platforms.toSet()) { text(p.name) }.children

        is TypeConstructor -> contentBuilder.contentFor(dri, platforms.toSet()) {
            link(p.dri.classNames.orEmpty(), p.dri)
        }.children + p.projections.flatMap { signature(it, dri, platforms) }

        is Variance -> contentBuilder.contentFor(dri, platforms.toSet()) {
            text(p.kind.toString() + " ")
        }.children + signature(p.inner, dri, platforms)

        is Star -> contentBuilder.contentFor(dri, platforms.toSet()) { text("*") }.children

        is Nullable -> signature(p.inner, dri, platforms) + contentBuilder.contentFor(
            dri,
            platforms.toSet()
        ) { text("?") }.children
    }

    private fun <T : Documentable> Collection<T>.filterOnPlatform(platformData: PlatformData) =
        this.filter { it.platformData.contains(platformData) }
}