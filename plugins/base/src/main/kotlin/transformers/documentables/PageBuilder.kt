package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.pages.*

open class DefaultPageBuilder(
    override val rootContentGroup: RootContentBuilder
) : PageBuilder {

    override fun pageForModule(m: Module): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m, m.packages.map { pageForPackage(it) })

    override fun pageForPackage(p: Package) =
        PackagePageNode(p.name, contentForPackage(p), setOf(p.dri), p,
            p.classlikes.map { pageForClasslike(it) } +
                    p.functions.map { pageForMember(it) })

    override fun pageForClasslike(c: Classlike): ClasslikePageNode {
        val constructors = when (c) {
            is Class -> c.constructors
            is Enum -> c.constructors
            else -> emptyList()
        }

        return ClasslikePageNode(c.name, contentForClasslike(c), setOf(c.dri), c,
            constructors.map { pageForMember(it) } +
                    c.classlikes.map { pageForClasslike(it) } +
                    c.functions.map { pageForMember(it) })
    }

    override fun pageForMember(m: CallableNode): MemberPageNode =
        when (m) {
            is Function ->
                MemberPageNode(m.name, contentForFunction(m), setOf(m.dri), m)
            else -> throw IllegalStateException("$m should not be present here")
        }

    protected open fun group(node: Documentable, content: PageContentBuilderFunction) =
        rootContentGroup(node, ContentKind.Main, content)

    protected open fun contentForModule(m: Module) = group(m) {
        header(1) { text("root") }
        block("Packages", 2, ContentKind.Packages, m.packages, m.platformData) {
            link(it.name, it.dri)
        }
        text("Index\n")
        text("Link to allpage here")
    }

    protected open fun contentForPackage(p: Package) = group(p) {
        header(1) { text("Package ${p.name}") }
        block("Types", 2, ContentKind.Properties, p.classlikes, p.platformData) {
            link(it.name, it.dri)
            text(it.briefDocTagString)
        }
        block("Functions", 2, ContentKind.Functions, p.functions, p.platformData) {
            link(it.name, it.dri)
            signature(it)
            text(it.briefDocTagString)
        }
    }

    open fun contentForClasslike(c: Classlike): ContentGroup = when (c) {
        is Class -> contentForClass(c)
        is Enum -> contentForEnum(c)
        else -> throw IllegalStateException("$c should not be present here")
    }

    protected fun contentForClass(c: Class) = group(c) {
        header(1) { text(c.name) }

        c.inherited.takeIf { it.isNotEmpty() }?.let {
            header(2) { text("SuperInterfaces") }
            linkTable(it)
        }
        contentForComments(c)
        block("Constructors", 2, ContentKind.Functions, c.constructors, c.platformData) {
            link(it.name, it.dri)
            signature(it)
            text(it.briefDocTagString)
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

    fun contentForEnum(c: Enum): ContentGroup = group(c) {
        header(1) { text("enum ${c.name}") }

        block("Entries", 2, ContentKind.Properties, c.entries, c.platformData) { entry ->
            link(entry.name, entry.dri)
            contentForComments(entry)
        }

        c.inherited.takeIf { it.isNotEmpty() }?.let {
            header(2) { text("SuperInterfaces") }
            linkTable(it)
        }
        contentForComments(c)
        block("Constructors", 2, ContentKind.Functions, c.constructors, c.platformData) {
            link(it.name, it.dri)
            signature(it)
            text(it.briefDocTagString)
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

    private fun PageContentBuilder.contentForComments(d: Documentable) =
        d.platformInfo.forEach { platformInfo ->
            platformInfo.documentationNode.children.forEach {
                header(3) {
                    text(it.toHeaderString())
                    text(" [${platformInfo.platformData.joinToString(", ") { it.platformType.name }}]")
                }
                comment(it.root)
                text("\n")
            }
        }

    private fun contentForFunction(f: Function) = group(f) {
        header(1) { text(f.name) }
        signature(f)
        contentForComments(f)
        block("Parameters", 2, ContentKind.Parameters, f.children, f.platformData) {
            text(it.name ?: "<receiver>")
            it.platformInfo.forEach { it.documentationNode.children.forEach { comment(it.root) } }
        }
    }

    private fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()
}

typealias RootContentBuilder = (Documentable, Kind, PageContentBuilderFunction) -> ContentGroup

interface PageBuilder {
    val rootContentGroup: RootContentBuilder
    fun pageForModule(m: Module): ModulePageNode
    fun pageForPackage(p: Package): PackagePageNode
    fun pageForMember(m: CallableNode): MemberPageNode
    fun pageForClasslike(c: Classlike): ClasslikePageNode
}