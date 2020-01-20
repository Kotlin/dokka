package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.TypeWrapper
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinAsJavaPageContentBuilder(
    private val dri: DRI,
    private val platformData: Set<PlatformData>,
    private val kind: Kind,
    private val commentsConverter: CommentsToContentConverter,
    val logger: DokkaLogger,
    private val styles: Set<Style> = emptySet(),
    private val extras: Set<Extra> = emptySet()
) {
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

    fun header(level: Int, block: KotlinAsJavaPageContentBuilderFunction) {
        contents += ContentHeader(level, group(ContentKind.Symbol, block))
    }

    fun text(text: String, kind: Kind = ContentKind.Symbol) {
        contents += createText(text, kind)
    }

    private fun signature(f: Function, block: KotlinAsJavaPageContentBuilderFunction) {
        contents += group(f.dri, f.platformData, ContentKind.Symbol, block)
    }

    fun signature(f: Function) = signature(f) {

        val returnType = f.returnType
        if (!f.isConstructor) {
            if (returnType != null &&
                returnType.constructorFqName != Unit::class.qualifiedName) {
                type(returnType)
                text(" ")
            } else text("void ")

        }

        link(f.name, f.dri)
        text("(")
        val params = listOfNotNull(f.receiver) + f.parameters
        list(params) {
            type(it.type)
            text(" ")
            link(it.name ?: "receiver", it.dri)
        }
        text(")")
    }

    fun linkTable(elements: List<DRI>) {
        contents += ContentTable(
            emptyList(),
            elements.map { group(dri, platformData, ContentKind.Classes) { link(it.classNames ?: "", it) } },
            DCI(dri, kind),
            platformData, styles, extras
        )
    }

    fun <T : Documentable> block(
        name: String,
        level: Int,
        kind: Kind,
        elements: Iterable<T>,
        platformData: Set<PlatformData>,
        operation: KotlinAsJavaPageContentBuilder.(T) -> Unit
    ) {
        header(level) { text(name) }

        contents += ContentTable(
            emptyList(),
            elements.map { group(it.dri, it.platformData, kind) { operation(it) } },
            DCI(dri, kind),
            platformData, styles, extras
        )
    }

    fun <T> list(
        elements: List<T>,
        prefix: String = "",
        suffix: String = "",
        separator: String = ",",
        operation: KotlinAsJavaPageContentBuilder.(T) -> Unit
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

    fun link(text: String, address: DRI, kind: Kind = ContentKind.Symbol) {
        contents += ContentDRILink(
            listOf(createText(text)),
            address,
            DCI(dri, kind),
            platformData
        )
    }

    fun link(address: DRI, kind: Kind, block: KotlinAsJavaPageContentBuilderFunction) {
        contents += ContentDRILink(
            group(ContentKind.Main, block).children,
            address,
            DCI(dri, kind),
            platformData
        )
    }

    fun comment(docTag: DocTag) {
        contents += group(ContentKind.Comment) {
            with(this as KotlinAsJavaPageContentBuilder) {
                contents += commentsConverter.buildContent(
                    docTag,
                    DCI(dri, ContentKind.Comment),
                    platformData
                )
            }
        }
    }

    fun group(kind: Kind, block: KotlinAsJavaPageContentBuilderFunction): ContentGroup =
        group(dri, platformData, kind, block)

    fun group(
        dri: DRI,
        platformData: Set<PlatformData>,
        kind: Kind,
        block: KotlinAsJavaPageContentBuilderFunction
    ): ContentGroup = group(dri, platformData, kind, commentsConverter, logger, block)

    companion object {
        fun group(
            dri: DRI,
            platformData: Set<PlatformData>,
            kind: Kind,
            commentsConverter: CommentsToContentConverter,
            logger: DokkaLogger,
            block: KotlinAsJavaPageContentBuilderFunction
        ): ContentGroup =
            KotlinAsJavaPageContentBuilder(dri, platformData, kind, commentsConverter, logger).apply(block).build()
    }
}

private fun KotlinAsJavaPageContentBuilder.type(t: TypeWrapper) {
    if (t.constructorNamePathSegments.isNotEmpty() && t.dri != null)
        link(t.constructorNamePathSegments.last(), t.dri!!)
    else (this as? KotlinAsJavaPageContentBuilder)?.let {
        logger.error("type $t cannot be resolved")
        text("???")
    }
    list(t.arguments, prefix = "<", suffix = ">") {
        type(it)
    }
}

typealias KotlinAsJavaPageContentBuilderFunction = KotlinAsJavaPageContentBuilder.() -> Unit