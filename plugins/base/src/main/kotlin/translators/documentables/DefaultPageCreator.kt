package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.doc.Property
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

open class DefaultPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    protected open val contentBuilder = PageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    open fun pageForModule(m: DModule) =
        ModulePageNode(m.name.ifEmpty { "<root>" }, contentForModule(m), m, m.packages.map(::pageForPackage))

    open fun pageForPackage(p: DPackage): PackagePageNode = PackagePageNode(
        p.name, contentForPackage(p), setOf(p.dri), p,
        p.classlikes.map(::pageForClasslike) +
                p.functions.map(::pageForFunction)
    )

    open fun pageForClasslike(c: DClasslike): ClasslikePageNode {
        val constructors = if (c is WithConstructors) c.constructors else emptyList()

        return ClasslikePageNode(
            c.name.orEmpty(), contentForClasslike(c), setOf(c.dri), c,
            constructors.map(::pageForFunction) +
                    c.classlikes.map(::pageForClasslike) +
                    c.functions.map(::pageForFunction)
        )
    }

    open fun pageForFunction(f: DFunction) = MemberPageNode(f.name, contentForFunction(f), setOf(f.dri), f)

    protected open fun contentForModule(m: DModule) = contentBuilder.contentFor(m) {
        header(1) { text(m.name) }
        block("Packages", 2, ContentKind.Packages, m.packages, m.platformData.toSet()) {
            link(it.name, it.dri)
        }
//        text("Index\n") TODO
//        text("Link to allpage here")
    }

    protected open fun contentForPackage(p: DPackage) = contentBuilder.contentFor(p) {
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
            group {
                +buildSignature(it)
                group(kind = ContentKind.BriefComment) {
                    text(it.briefDocumentation())
                }
            }
        }
        block("Functions", 2, ContentKind.Functions, s.functions, platformData.toSet()) {
            link(it.name, it.dri)
            group {
                +buildSignature(it)

                group(kind = ContentKind.BriefComment) {
                    text(it.briefDocumentation())
                }
            }
        }
        block("Properties", 2, ContentKind.Properties, s.properties, platformData.toSet()) {
            link(it.name, it.dri)
            +buildSignature(it)
            breakLine()
            group(kind = ContentKind.BriefComment) {
                text(it.briefDocumentation())
            }
        }
        (s as? WithExtraProperties<Documentable>)?.let { it.extra[InheritorsInfo] }?.let { inheritors ->
            val map = inheritors.value.map
            text("Subclasses:")
            platformDependentHint(dri = s.dri, platformData = map.keys) {
                map.keys.forEach { pd ->
                    linkTable(map[pd].orEmpty(), ContentKind.Classlikes, setOf(pd))
                }
            }
        }
    }

    protected open fun contentForClasslike(c: DClasslike) = contentBuilder.contentFor(c) {
        header(1) { text(c.name.orEmpty()) }
        +buildSignature(c)

        +contentForComments(c) { it !is Property }

        if (c is WithConstructors) {
            block(
                "Constructors",
                2,
                ContentKind.Constructors,
                c.constructors.filter { it.extra[PrimaryConstructorExtra] == null },
                c.platformData.toSet()
            ) {
                link(it.name, it.dri)
                group {
                    +buildSignature(it)

                    group(kind = ContentKind.BriefComment) {
                        text(it.briefDocumentation())
                    }
                }
            }
        }

        +contentForScope(c, c.dri, c.platformData)
    }

    protected open fun contentForComments(
        d: Documentable,
        filtering: (TagWrapper) -> Boolean = { true }
    ) = contentBuilder.contentFor(d) {
        // TODO: this probably needs fixing
        d.documentation.forEach { _, documentationNode ->
            documentationNode.children.filter(filtering).forEach {
                header(3) {
                    text(it.toHeaderString())
                    d.documentation.keys.joinToString(prefix = "[", postfix = "]", separator = ", ")
                }
                comment(it.root)
                text("\n")
            }
        }
    }.children

    protected open fun contentForFunction(f: DFunction) = contentBuilder.contentFor(f) {
        header(1) { text(f.name) }
        +buildSignature(f)
        +contentForComments(f)
    }

    protected open fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()

    protected open fun Documentable.briefDocumentation() =
        documentation.values
            .firstOrNull()
            ?.children
            ?.firstOrNull()
            ?.root
            ?.docTagSummary() ?: ""
}