package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.parseMarkdown
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType


class DefaultDocumentationToPageTransformer(
    private val markdownConverter: MarkdownToContentConverter,
    private val logger: DokkaLogger
) : DocumentationToPageTransformer {
    override fun transform(passConfiguration: DokkaConfiguration.PassConfiguration, module: Module): ModulePageNode {
        val platformData = passConfiguration.targets.map { PlatformData(it, passConfiguration.analysisPlatform) }
        return PageBuilder(platformData).pageForModule(module)
    }

    private inner class PageBuilder(private val platformData: List<PlatformData>) {
        fun pageForModule(m: Module) =
            ModulePageNode("root", contentForModule(m), documentationNode = m).apply {
                // TODO change name
                appendChildren(m.packages.map { pageForPackage(it, this) })
            }

        private fun pageForPackage(p: Package, parent: PageNode) =
            PackagePageNode(p.name, contentForPackage(p), parent, p.dri, p).apply {
                appendChildren(p.classes.map { pageForClass(it, this) })
                appendChildren(p.functions.map { pageForMember(it, this) })
                appendChildren(p.properties.map { pageForMember(it, this) })
            }

        private fun pageForClass(c: Class, parent: PageNode): ClassPageNode =
            ClassPageNode(c.name, contentForClass(c), parent, c.dri, c).apply {
                appendChildren(c.constructors.map { pageForMember(it, this) })
                appendChildren(c.classes.map { pageForClass(it, this) })
                appendChildren(c.functions.map { pageForMember(it, this) })
                appendChildren(c.properties.map { pageForMember(it, this) })
            }

        private fun pageForMember(m: CallableNode<*>, parent: PageNode): MemberPageNode =
            when (m) {
                is Function -> MemberPageNode(m.name, contentForFunction(m), parent, m.dri, m)
                is Property -> MemberPageNode(m.name, emptyList(), parent, m.dri, m)
                else -> throw IllegalStateException("$m should not be present here")
            }

        private fun contentForModule(m: Module) = content(DCI(m.dri, platformData)) {
            header(1) { text("root") }
            block("Packages", m.packages) { link(it.name, it.dri) }
            text("Index\n")
            text("Link to allpage here")
        }

        private fun contentForPackage(p: Package) = content(DCI(p.dri, platformData)) {
            header(1) { text("Package ${p.name}") }
            block("Types", p.classes) {
                link(it.name, it.dri)
                text(it.briefDocstring)
            }
            block("Functions", p.functions) {
                link(it.name, it.dri)
                signature(it)
                text(it.briefDocstring)
            }
        }

        private fun contentForClass(c: Class) = content(DCI(c.dri, platformData)) {
            header(1) { text(c.name) }
            c.rawDocstrings.forEach { markdown(it, c) }
            block("Constructors", c.constructors) {
                link(it.name, it.dri)
                signature(it)
                text(it.briefDocstring)
            }
            block("Functions", c.functions) {
                link(it.name, it.dri)
                signature(it)
                text(it.briefDocstring)
            }
        }

        private fun contentForFunction(f: Function) = content(DCI(f.dri, platformData)) {
            header(1) { text(f.name) }
            signature(f)
            f.rawDocstrings.forEach { markdown(it, f) }
            block("Parameters", f.children) { param ->
                group {
                    text(param.name ?: "<receiver>")
                    param.rawDocstrings.forEach { markdown(it, param) }
                }
            }
        }
    }

    // TODO: Make some public builder or merge it with page builder, whateva
    private inner class ContentBuilder(private val dci: DCI) {
        private val contents = mutableListOf<ContentNode>()

        fun build() = contents.toList() // should include nodes coalescence

        inline fun header(level: Int, block: ContentBuilder.() -> Unit) {
            contents += ContentHeader(content(block), level, dci)
        }

        fun text(text: String) {
            contents += ContentText(text, dci)
        }

        inline fun symbol(block: ContentBuilder.() -> Unit) {
            contents += ContentSymbol(content(block), dci)
        }

        inline fun <T: DocumentationNode<*>> block(name: String, elements: Iterable<T>, block: ContentBuilder.(T) -> Unit) {
            contents += ContentBlock(name, elements.flatMap { content(dci.copy(dri = it.dri)) { group { block(it) } } }, dci)
        }

        inline fun group(block: ContentBuilder.() -> Unit) {
            contents += ContentGroup(content(block), dci)
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
            contents += ContentLink(text, address, dci)
        }

        fun markdown(raw: String, node: DocumentationNode<*>) {
            contents += markdownConverter.buildContent(parseMarkdown(raw), dci, node)
        }

        private inline fun content(block: ContentBuilder.() -> Unit): List<ContentNode> = content(dci, block)
    }

    private inline fun content(dci: DCI, block: ContentBuilder.() -> Unit): List<ContentNode> =
        ContentBuilder(dci).apply(block).build()

    // When builder is made public it will be moved as extension method to someplace near Function model
    private fun ContentBuilder.signature(f: Function) = symbol {
        text("fun ")
        if (f.receiver is Parameter) {
            type(f.receiver.descriptors.first().type)
            text(".")
        }
        link(f.name, f.dri)
        text("(")
        list(f.parameters) {
            link(it.name!!, it.dri)
            text(": ")
            type(it.descriptors.first().type)
        }
        text(")")
        val returnType = f.descriptors.first().returnType
        if (f.descriptors.first() !is ConstructorDescriptor && returnType != null &&
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