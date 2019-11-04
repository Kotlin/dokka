package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.parseMarkdown
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe


class DefaultDocumentationToPageTransformer(
    private val markdownConverter: MarkdownToContentConverter
) : DocumentationToPageTransformer {
    override fun transform(passConfiguration: DokkaConfiguration.PassConfiguration, module: Module): PageNode {
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
                // TODO: Pages for constructors
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

        private fun contentForModule(m: Module) = content(platformData) {
            header(1) { text("root") }
            block("Packages", m.packages) { link(it.name, it.dri) }
            text("Index")
            text("Link to allpage here")
        }

        private fun contentForPackage(p: Package) = content(platformData) {
            header(1) { text("Package ${p.name}") }
            block("Types", p.classes) {
                link(it.name, it.dri)
                text(it.briefDocstring)
                text("signature for class")
            }
            block("Functions", p.functions) {
                link(it.name, it.dri)
                text(it.briefDocstring)
                text("signature for function")
            }
        }

        private fun contentForClass(c: Class) = content(platformData) {
            header(1) { text(c.name) }
            markdown(c.rawDocstring, c)
            text("PING PAWEL TO ADD CONSTRUCTORS TO MODEL!!!")
            block("Constructors", emptyList<Function>() /* TODO: CONSTRUCTORS*/) {
                link(it.name, it.dri)
                text(it.briefDocstring)
                text("message to Pawel from the future: you forgot about extracting constructors, didn't you?")
            }
            block("Functions", c.functions) {
                link(it.name, it.dri)
                text(it.briefDocstring)
                text("signature for function")
            }
        }

        private fun contentForFunction(f: Function) = content(platformData) {
            header(1) { text(f.name) }
            text("signature for function")
            markdown(f.rawDocstring, f)
            block("Parameters", f.children) {
                group {
                    text(it.name ?: "RECEIVER")
                    markdown(it.rawDocstring, it)
                }
            }
        }
    }

    // TODO: Make some public builder or merge it with page builder, whateva
    private inner class ContentBuilder(private val platformData: List<PlatformData>) {
        private val contents = mutableListOf<ContentNode>()

        fun build() = contents.toList()

        fun header(level: Int, block: ContentBuilder.() -> Unit) {
            contents += ContentHeader(content(block), level, platformData)
        }

        fun text(text: String) {
            contents += ContentText(text, platformData)
        }

        fun <T> block(name: String, elements: Iterable<T>, block: ContentBuilder.(T) -> Unit) {
            contents += ContentBlock(name, content { elements.forEach { block(it) } }, platformData)
        }

        fun group(block: ContentBuilder.() -> Unit) {
            contents += ContentGroup(content(block), platformData)
        }

        fun link(text: String, address: DRI) {
            contents += ContentLink(text, address, platformData)
        }

        fun markdown(raw: String, node: DocumentationNode<*>) {
            contents += markdownConverter.buildContent(parseMarkdown(raw), platformData, node)
        }

        private fun content(block: ContentBuilder.() -> Unit): List<ContentNode> = content(platformData, block)
    }

    private fun content(platformData: List<PlatformData>, block: ContentBuilder.() -> Unit): List<ContentNode> =
        ContentBuilder(platformData).apply(block).build()

}

fun DocumentationNode<*>.identifier(platformData: List<PlatformData>): List<ContentNode> {
//    when(this) {
//        is Class -> ContentText(this.descriptor.toString(), platforms), ContentText("(") this.properties.map { ContentText(it.descriptor.visibility + " " + it.descriptor.name + ":" + ),}
//        is Function ->
//        is Property ->
//        else -> return emptyList()
//    }
    TODO()
}
// take this ^ from old dokka
/*
pages are equal if the content and the children are equal
we then can merge the content by merging the platforms
and take an arbitrary set of the children
but we need to recursively process all of the children anyway
 */