package org.jetbrains.dokka.pages

import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.Parameter
import org.jetbrains.dokka.model.TypeWrapper
import org.jetbrains.dokka.links.DRI

class DefaultPageContentBuilder(
    private val dri: DRI,
    private val platformData: Set<PlatformData>,
    private val kind: Kind,
    private val commentsConverter: CommentsToContentConverter,
    val logger: DokkaLogger,
    private val styles: Set<Style> = emptySet(),
    private val extras: Set<Extra> = emptySet()
) : PageContentBuilder {
    private val contents = mutableListOf<ContentNode>()

    private fun createText(text: String, kind: Kind = ContentKind.Symbol) =
        ContentText(text, DCI(dri, kind), platformData, styles, extras)

    private fun build() = ContentGroup(
        contents.toList(),
        DCI(dri, kind),
        platformData,
        styles,
        extras
    )

    override fun header(level: Int, block: PageContentBuilderFunction) {
        contents += ContentHeader(level, group(ContentKind.Symbol, block))
    }

    override fun text(text: String, kind: Kind) {
        contents += createText(text, kind)
    }

    private fun signature(f: Function, block: PageContentBuilderFunction) {
        contents += group(f.dri, f.platformData, ContentKind.Symbol, block)
    }

    override fun signature(f: Function) = signature(f) {
        text("fun ")
        if (f.receiver is Parameter) {
            type(f.receiver.type)
            text(".")
        }
        link(f.name, f.dri)
        text("(")
        list(f.parameters) {
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
            elements.map { group(dri, platformData, ContentKind.Classes) { link(it.classNames ?: "", it) } },
            DCI(dri, kind),
            platformData, styles, extras
        )
    }

    override fun <T : Documentable> block(
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
            elements.map { group(it.dri, it.platformData, kind) { operation(it) } },
            DCI(dri, kind),
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

    override fun link(text: String, address: DRI, kind: Kind) {
        contents += ContentDRILink(
            listOf(createText(text)),
            address,
            DCI(dri, kind),
            platformData
        )
    }

    override fun link(address: DRI, kind: Kind, block: PageContentBuilderFunction) {
        contents += ContentDRILink(
            group(ContentKind.Main, block).children,
            address,
            DCI(dri, kind),
            platformData
        )
    }

    override fun comment(docTag: DocTag) {
        contents += group(ContentKind.Comment) {
            with(this as DefaultPageContentBuilder) {
                contents += commentsConverter.buildContent(
                    docTag,
                    DCI(dri, ContentKind.Comment),
                    platformData
                )
            }
        }
    }

    fun group(kind: Kind, block: PageContentBuilderFunction): ContentGroup =
        group(dri, platformData, kind, block)

    override fun group(
        dri: DRI,
        platformData: Set<PlatformData>,
        kind: Kind,
        block: PageContentBuilderFunction
    ): ContentGroup = group(dri, platformData, kind, commentsConverter, logger, block)

    companion object {
        fun group(
            dri: DRI,
            platformData: Set<PlatformData>,
            kind: Kind,
            commentsConverter: CommentsToContentConverter,
            logger: DokkaLogger,
            block: PageContentBuilderFunction
        ): ContentGroup =
            DefaultPageContentBuilder(dri, platformData, kind, commentsConverter, logger).apply(block).build()
    }
}


private fun PageContentBuilder.type(t: TypeWrapper) {
    if (t.constructorNamePathSegments.isNotEmpty() && t.dri != null)
        link(t.constructorNamePathSegments.last(), t.dri!!)
    else if (t.constructorNamePathSegments.isNotEmpty() && t.dri == null)
        text(t.toString())
    else (this as? DefaultPageContentBuilder)?.let {
            logger.error("type $t cannot be resolved")
            text("???")
        }

    list(t.arguments, prefix = "<", suffix = ">") {
        type(it)
    }
}

typealias PageContentBuilderFunction = PageContentBuilder.() -> Unit

@DslMarker
annotation class ContentMarker

@ContentMarker
interface PageContentBuilder {
    fun group(
        dri: DRI,
        platformData: Set<PlatformData>,
        kind: Kind, block: PageContentBuilderFunction): ContentGroup
    fun text(text: String, kind: Kind = ContentKind.Symbol)
    fun signature(f: Function)
    fun link(text: String, address: DRI, kind: Kind = ContentKind.Symbol)
    fun link(address: DRI, kind: Kind = ContentKind.Symbol, block: PageContentBuilderFunction)
    fun linkTable(elements: List<DRI>)
    fun comment(docTag: DocTag)
    fun header(level: Int, block: PageContentBuilderFunction)
    fun <T> list(
        elements: List<T>,
        prefix: String =  "",
        suffix: String =  "",
        separator: String = ",",
        operation: PageContentBuilder.(T) -> Unit
    )

    fun <T : Documentable> block(
        name: String,
        level: Int,
        kind: Kind,
        elements: Iterable<T>,
        platformData: Set<PlatformData>,
        operation: PageContentBuilder.(T) -> Unit
    )
}