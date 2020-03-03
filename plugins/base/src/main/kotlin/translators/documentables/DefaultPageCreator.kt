package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Annotation
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

open class DefaultPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    protected open val contentBuilder = PageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    open fun pageForModule(m: Module) =
        ModulePageNode(m.name.ifEmpty { "<root>" }, contentForModule(m), m, m.packages.map(::pageForPackage))

    open fun pageForPackage(p: Package): PackagePageNode = PackagePageNode(
        p.name, contentForPackage(p), setOf(p.dri), p,
        p.classlikes.map(::pageForClasslike) +
                p.functions.map(::pageForFunction) +
                p.packages.map(::pageForPackage)
    )

    open fun pageForClasslike(c: Classlike): ClasslikePageNode {
        val constructors = if (c is WithConstructors) c.constructors else emptyList()

        return ClasslikePageNode(
            c.name.orEmpty(), contentForClasslike(c), setOf(c.dri), c,
            constructors.map(::pageForFunction) +
                    c.classlikes.map(::pageForClasslike) +
                    c.functions.map(::pageForFunction)
        )
    }

    open fun pageForFunction(f: Function) = MemberPageNode(f.name, contentForFunction(f), setOf(f.dri), f)

    protected open fun contentForModule(m: Module) = contentBuilder.contentFor(m) {
        header(1) { text(m.name) }
        block("Packages", 2, ContentKind.Packages, m.packages, m.platformData.toSet()) {
            link(it.name, it.dri)
        }
        text("Index\n")
        text("Link to allpage here")
    }

    protected open fun contentForPackage(p: Package) = contentBuilder.contentFor(p) {
        header(1) { text("Package ${p.name}") }
        +contentForScope(p, p.dri, p.platformData)
    }

    protected open fun contentForScope(
        s: WithScope,
        dri: DRI,
        platformData: List<PlatformData>
    ) = contentBuilder.contentFor(s as Documentable) {
        block("Types", 2, ContentKind.Classlikes, s.classlikes, platformData.toSet()) {
            link(it.name.orEmpty(), it.dri)
            +buildSignature(it)
            text(it.briefDocTagString)
        }
        block("Functions", 2, ContentKind.Functions, s.functions, platformData.toSet()) {
            link(it.name, it.dri)
            +buildSignature(it)
            text(it.briefDocTagString)
        }
        block("Properties", 2, ContentKind.Properties, s.properties, platformData.toSet()) {
            link(it.name, it.dri)
            text(it.briefDocTagString)
        }
    }

    protected open fun contentForClasslike(c: Classlike) = contentBuilder.contentFor(c) {
        +buildSignature(c)
        +contentForComments(c)

        if (c is WithConstructors) {
            block("Constructors", 2, ContentKind.Constructors, c.constructors, c.platformData.toSet()) {
                link(it.name, it.dri)
                +buildSignature(it)
                text(it.briefDocTagString)
            }
        }

        +contentForScope(c, c.dri, c.platformData)
    }

    protected open fun contentForComments(d: Documentable) = contentBuilder.contentFor(d) {
        // TODO: this probably needs fixing
        d.documentation.forEach { _, documentationNode ->
            documentationNode.children.forEach {
                header(3) {
                    text(it.toHeaderString())
                    d.documentation.keys.joinToString(prefix = "[", postfix = "]", separator = ", ")
                }
                comment(it.root)
                text("\n")
            }
        }
    }.children

    protected open fun contentForFunction(f: Function) = contentBuilder.contentFor(f) {
        header(1) { text(f.name) }
        +buildSignature(f)
        +contentForComments(f)
        block("Parameters", 2, ContentKind.Parameters, f.children, f.platformData.toSet()) {
            text(it.name ?: "<receiver>")
            it.documentation.forEach { it.value.children.forEach { comment(it.root) } }
        }
    }

    protected open fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()
}