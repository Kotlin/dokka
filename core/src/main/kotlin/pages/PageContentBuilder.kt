package org.jetbrains.dokka.pages

import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.Model.Function
import org.jetbrains.dokka.Model.Parameter
import org.jetbrains.dokka.Model.TypeWrapper
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.parseMarkdown

class DefaultPageContentBuilder(
    private val node: DocumentationNode,
    private val kind: Kind,
    private val markdownConverter: MarkdownToContentConverter,
    val logger: DokkaLogger,
    private val styles: Set<Style> = emptySet(),
    private val extras: Set<Extra> = emptySet()
) : PageContentBuilder {
    private val contents = mutableListOf<ContentNode>()

    private fun createText(text: String) =
        ContentText(text, DCI(node.dri, ContentKind.Symbol), node.platformData, styles, extras)

    private fun build() = ContentGroup(
        contents.toList(),
        DCI(node.dri, kind),
        node.platformData,
        styles,
        extras
    )

    override fun header(level: Int, block: PageContentBuilderFunction) {
        contents += ContentHeader(level, group(ContentKind.Symbol, block))
    }

    override fun text(text: String) {
        contents += createText(text)
    }

    private fun signature(f: Function, block: PageContentBuilderFunction) {
        contents += group(f, ContentKind.Symbol, block)
    }

    override fun signature(f: Function) = signature(f) {
        text("fun ")
        if (f.receiver is Parameter) {
            type(f.receiver.type)
            text(".")
        }
        link(f.name, f.dri)
        text("(")
        list(f.parameters, "", "", ", ") {
            link(it.name!!, it.dri)
            text(": ")
            type(it.type)
        }
        text(")")
        val returnType = f.returnType
        if (!f.isConstructor && returnType != null &&
            returnType.constructorFqName != Unit::class.qualifiedName) {
            text(": ")
            type(returnType)
        }
    }

    override fun linkTable(elements: List<DRI>) {
        contents += ContentTable(
            emptyList(),
            elements.map { group(node, ContentKind.Classes) { link(it.classNames ?: "", it) } },
            DCI(node.dri, kind),
            node.platformData, styles, extras
        )
    }

    override fun <T : DocumentationNode> block(
        name: String,
        level: Int,
        kind: Kind,
        elements: Iterable<T>,
        platformData: Set<PlatformData>,
        operation: PageContentBuilder.(T) -> Unit
    ) {
        header(level) { text(name) }

        contents += ContentTable(
            emptyList(),
            elements.map { group(it, kind) { operation(it) } },
            DCI(node.dri, kind),
            platformData, styles, extras
        )
    }

    override fun <T> list(
        elements: List<T>,
        prefix: String,
        suffix: String,
        separator: String,
        operation: PageContentBuilder.(T) -> Unit
    ) {
        if (elements.isNotEmpty()) {
            if (prefix.isNotEmpty()) text(prefix)
            elements.dropLast(1).forEach {
                operation(it)
                text(separator)
            }
            operation(elements.last())
            if (suffix.isNotEmpty()) text(suffix)
        }
    }

    override fun link(text: String, address: DRI) {
        contents += ContentDRILink(
            listOf(createText(text)),
            address,
            DCI(node.dri, ContentKind.Symbol),
            node.platformData
        )
    }

    override fun comment(raw: String, links: Map<String, DRI>) {
        contents += group(ContentKind.Comment) {
            with(this as DefaultPageContentBuilder) {
                contents += markdownConverter.buildContent(
                    parseMarkdown(raw),
                    DCI(node.dri, ContentKind.Comment),
                    node.platformData,
                    links
                )
            }
        }
    }

    override fun markdown(raw: String, links: Map<String, DRI>) {
        contents += markdownConverter.buildContent(
            parseMarkdown(raw), DCI(node.dri, ContentKind.Sample),
            node.platformData,
            links
        )
    }

    private fun group(kind: Kind, block: PageContentBuilderFunction): ContentGroup =
        group(node, kind, block)

    override fun group(
        node: DocumentationNode,
        kind: Kind,
        block: PageContentBuilderFunction
    ): ContentGroup = group(node, kind, markdownConverter, logger, block)

    companion object {
        fun group(
            node: DocumentationNode,
            kind: Kind,
            markdownConverter: MarkdownToContentConverter,
            logger: DokkaLogger,
            block: PageContentBuilderFunction
        ): ContentGroup =
            DefaultPageContentBuilder(node, kind, markdownConverter, logger).apply(block).build()
    }
}


private fun PageContentBuilder.type(t: TypeWrapper) {
    if (t.constructorNamePathSegments.isNotEmpty() && t.dri != null)
        link(t.constructorNamePathSegments.last(), t.dri!!)
    else (this as? DefaultPageContentBuilder)?.let {
            logger.error("type $t cannot be resolved")
            text("???")
        }
    list(t.arguments, prefix = "<", suffix = ">", separator = ", ") {
        type(it)
    }
}

typealias PageContentBuilderFunction = PageContentBuilder.() -> Unit

@DslMarker
annotation class ContentMarker

@ContentMarker
interface PageContentBuilder {
    fun group(node: DocumentationNode, kind: Kind, block: PageContentBuilderFunction): ContentGroup
    fun text(text: String)
    fun signature(f: Function)
    fun link(text: String, address: DRI)
    fun linkTable(elements: List<DRI>)
    fun comment(raw: String, links: Map<String, DRI>)
    fun markdown(raw: String, links: Map<String, DRI>)
    fun header(level: Int, block: PageContentBuilder.() -> Unit)
    fun <T> list(
        elements: List<T>,
        prefix: String,
        suffix: String,
        separator: String,
        operation: PageContentBuilder.(T) -> Unit
    )

    fun <T : DocumentationNode> block(
        name: String,
        level: Int,
        kind: Kind,
        elements: Iterable<T>,
        platformData: Set<PlatformData>,
        operation: PageContentBuilder.(T) -> Unit
    )
}