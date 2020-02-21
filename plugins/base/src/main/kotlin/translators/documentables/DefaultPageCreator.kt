package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

open class DefaultPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    val logger: DokkaLogger
) {
    protected open val contentBuilder = PageContentBuilder(commentsToContentConverter, logger)

    open fun pageForModule(m: Module): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m, m.packages.map { pageForPackage(it) })

    open fun pageForPackage(p: Package): PackagePageNode =
        PackagePageNode(p.name, contentForPackage(p), setOf(p.dri), p,
            p.classlikes.map { pageForClasslike(it) } +
                    p.functions.map { pageForMember(it) } +
                    p.packages.map { pageForPackage(it) })

    open fun pageForClasslike(c: Classlike): ClasslikePageNode {
        val constructors = when (c) {
            is Class -> c.constructors
            is Enum -> c.constructors
            else -> emptyList()
        }

        return ClasslikePageNode((c as Documentable).name.orEmpty(), contentForClasslike(c), setOf(c.dri), c,
            constructors.map { pageForMember(it) } +
                    c.classlikes.map { pageForClasslike(it) } +
                    c.functions.map { pageForMember(it) })
    }

    open fun pageForMember(c: Callable): MemberPageNode = when (c) {
        is Function -> MemberPageNode(c.name, contentForFunction(c), setOf(c.dri), c)
        else -> throw IllegalStateException("$c should not be present here")
    }

    protected open fun contentForModule(m: Module) = contentBuilder.contentFor(m) {
        header(1) { text("root") }
        block("Packages", 2, ContentKind.Packages, m.packages, m.platformData) {
            link(it.name, it.dri)
        }
        text("Index\n")
        text("Link to allpage here")
    }

    protected open fun contentForPackage(p: Package) = contentBuilder.contentFor(p) {
        header(1) { text("Package ${p.name}") }
        block("Types", 2, ContentKind.Properties, p.classlikes as List<Documentable>, p.platformData) {
            link(it.name.orEmpty(), it.dri)
            text(it.briefDocTagString)
        }
        block("Functions", 2, ContentKind.Functions, p.functions, p.platformData) {
            link(it.name, it.dri)
            signature(it)
            text(it.briefDocTagString)
        }
    }

    protected open fun contentForClasslike(c: Classlike) = contentBuilder.contentFor(c as Documentable) {
        when (c) { // TODO this when will be removed when signature generation is moved to utils
            is Class -> header(1) { text(c.name) }
            is Enum -> {
                header(1) { text("enum ${c.name}") }
                block("Entries", 2, ContentKind.Properties, c.entries, c.platformData) { entry ->
                    link(entry.name.orEmpty(), entry.dri)
                    contentForComments(entry)
                }
            }
            else -> throw IllegalStateException("$c should not be present here")
        }

        contentForComments(c as Documentable)

        if (c is WithConstructors) {
            block("Constructors", 2, ContentKind.Functions, c.constructors, c.platformData) {
                link(it.name, it.dri)
                signature(it)
                text(it.briefDocTagString)
            }
        }

        block("Functions", 2, ContentKind.Functions, c.functions, c.platformData) {
            link(it.name, it.dri)
            signature(it)
            text(it.briefDocTagString)
        }

        block("Properties", 2, ContentKind.Properties, c.properties, c.platformData) {
            link(it.name, it.dri)
            text(it.briefDocTagString)
        }
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
    }

    protected open fun contentForFunction(f: Function) = contentBuilder.contentFor(f) {
        header(1) { text(f.name) }
        signature(f)
        contentForComments(f)
        block("Parameters", 2, ContentKind.Parameters, f.children, f.platformData) {
            text(it.name ?: "<receiver>")
            it.documentation.forEach { it.value.children.forEach { comment(it.root) } }
        }
    }

    protected open fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()
}
