package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.pages.Extra
import org.jetbrains.dokka.parseMarkdown
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType


class DefaultDocumentationToPageTransformer(
    private val markdownConverter: MarkdownToContentConverter,
    private val logger: DokkaLogger
) : DocumentationToPageTransformer {
    override fun transform(module: Module): ModulePageNode =
        PageBuilder().pageForModule(module)

    private inner class PageBuilder {
        fun pageForModule(m: Module): ModulePageNode =
            ModulePageNode("root", contentForModule(m), m, m.packages.map { pageForPackage(it) })

        private fun pageForPackage(p: Package) =
            PackagePageNode(p.name, contentForPackage(p), p.dri, p,
                p.classes.map { pageForClass(it) } +
                        p.functions.map { pageForMember(it) } +
                        p.properties.map { pageForMember(it) })

        private fun pageForClass(c: Class): ClassPageNode =
            ClassPageNode(c.name, contentForClass(c), c.dri, c,
                c.constructors.map { pageForMember(it) } +
                        c.classes.map { pageForClass(it) } +
                        c.functions.map { pageForMember(it) })

        private fun pageForMember(m: CallableNode<*>): MemberPageNode =
            when (m) {
                is Function ->
                    MemberPageNode(m.name, contentForFunction(m), m.dri, m)
                else -> throw IllegalStateException("$m should not be present here")
            }

        private fun contentForModule(m: Module) = group(m) {
            header(1) { text("root") }
            block("Packages", 2, ContentKind.Packages, m.packages, m.platformData) {
                link(it.name, it.dri)
            }
            text("Index\n")
            text("Link to allpage here")
        }

        private fun contentForPackage(p: Package) = group(p) {
            header(1) { text("Package ${p.name}") }
            block("Types", 2, ContentKind.Properties, p.classes, p.platformData) {
                link(it.name, it.dri)
                text(it.briefDocstring)
            }
            block("Functions", 2, ContentKind.Functions, p.functions, p.platformData) {
                link(it.name, it.dri)
                signature(it)
                text(it.briefDocstring)
            }
        }

        private fun contentForClass(c: Class) = group(c) {
            header(1) { text(c.name) }
            c.commentsData.forEach { (doc, links) -> comment(doc, links) }
            block("Constructors", 2, ContentKind.Functions, c.constructors, c.platformData) {
                link(it.name, it.dri)
                signature(it)
                text(it.briefDocstring)
            }
            block("Functions", 2, ContentKind.Functions, c.functions, c.platformData) {
                link(it.name, it.dri)
                signature(it)
                text(it.briefDocstring)
            }
        }

        private fun contentForFunction(f: Function) = group(f) {
            header(1) { text(f.name) }
            signature(f)
            f.commentsData.forEach { (doc, links) -> markdown(doc, links) }
            block("Parameters", 2, ContentKind.Parameters, f.children, f.platformData) {
                text(it.name ?: "<receiver>")
                it.commentsData.forEach { (doc, links) -> markdown(doc, links) }
            }
        }
    }

    // TODO: Make some public builder or merge it with page builder, whateva
    private inner class ContentBuilder(
        val node: DocumentationNode<*>,
        val kind: Kind,
        val styles: Set<Style> = emptySet(),
        val extras: Set<Extra> = emptySet()
    ) {
        private val contents = mutableListOf<ContentNode>()

        fun build() = ContentGroup(
            contents.toList(),
            DCI(node.dri, kind),
            node.platformData,
            styles,
            extras
        )

        fun header(level: Int, block: ContentBuilder.() -> Unit) {
            contents += ContentHeader(level, group(ContentKind.Symbol, block))
        }

        private fun createText(text: String) =
            ContentText(text, DCI(node.dri, ContentKind.Symbol), node.platformData, styles, extras)

        fun text(text: String) {
            contents += createText(text)
        }

        inline fun signature(f: Function, block: ContentBuilder.() -> Unit) {
            contents += group(f, ContentKind.Symbol, block)
        }

        inline fun <T : DocumentationNode<*>> block(
            name: String,
            level: Int,
            kind: Kind,
            elements: Iterable<T>,
            platformData: Set<PlatformData>,
            operation: ContentBuilder.(T) -> Unit
        ) {
            header(level) { text(name) }

            contents += ContentTable(
                emptyList(),
                elements.map { group(it, kind) { operation(it) } },
                DCI(node.dri, kind),
                platformData, styles, extras
            )
        }

        inline fun <T> list(
            elements: List<T>,
            prefix: String = "",
            suffix: String = "",
            separator: String = ", ",
            block: ContentBuilder.(T) -> Unit
        ) {
            if (elements.isNotEmpty()) {
                if (prefix.isNotEmpty()) text(prefix)
                elements.dropLast(1).forEach {
                    block(it)
                    text(separator)
                }
                block(elements.last())
                if (suffix.isNotEmpty()) text(suffix)
            }
        }

        fun link(text: String, address: DRI) {
            contents += ContentDRILink(
                listOf(createText(text)),
                address,
                DCI(node.dri, ContentKind.Symbol),
                node.platformData
            )
        }

        fun comment(raw: String, links: Map<String, DRI>) {
            contents += group(ContentKind.Comment) {
                contents += markdownConverter.buildContent(
                    parseMarkdown(raw),
                    DCI(node.dri, ContentKind.Comment),
                    node.platformData,
                    links
                )
            }
        }

        fun markdown(raw: String, links: Map<String, DRI>) {
            contents += markdownConverter.buildContent(
                parseMarkdown(raw), DCI(node.dri, ContentKind.Sample),
                node.platformData,
                links
            )
        }

        private inline fun group(kind: Kind, block: ContentBuilder.() -> Unit): ContentGroup =
            group(node, kind, block)
    }

    private inline fun group(
        node: DocumentationNode<*>,
        kind: Kind = ContentKind.Main,
        block: ContentBuilder.() -> Unit
    ) = ContentBuilder(node, kind).apply(block).build()

    // When builder is made public it will be moved as extension method to someplace near Function model
    private fun ContentBuilder.signature(f: Function) = signature(f) { // TODO: wrap this in ContentCode
        text("fun ")
        if (f.receiver is Parameter) {
            type(f.receiver.descriptors.first().descriptor.type)
            text(".")
        }
        link(f.name, f.dri)
        text("(")
        list(f.parameters) {
            link(it.name!!, it.dri)
            text(": ")
            type(it.descriptors.first().descriptor.type)
        }
        text(")")
        val returnType = f.descriptors.first().descriptor.returnType
        if (f.descriptors.first().descriptor !is ConstructorDescriptor && returnType != null &&
            returnType.constructor.declarationDescriptor?.fqNameSafe?.asString() != Unit::class.qualifiedName) {
            text(": ")
            type(returnType)
        }
    }

    private fun ContentBuilder.type(t: KotlinType) {
        t.constructor.declarationDescriptor?.also { link(it.fqNameSafe.pathSegments().last().asString(), DRI.from(it)) }
                ?: run {
                    logger.error("type $t cannot be resolved")
                    text("???")
                }
        list(t.arguments, prefix = "<", suffix = ">") {
            type(it.type)
        }
    }
}