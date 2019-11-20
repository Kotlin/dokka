package org.jetbrains.dokka.xml

import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*

internal class ContentBuilder(
    val platforms: Set<PlatformData>,
    val dri: DRI,
    val kind: Kind,
    val styles: Set<Style> = emptySet(),
    val extras: Set<Extra> = emptySet()
) {
    private val contents = mutableListOf<ContentNode>()

    fun build() = ContentGroup(
        contents.toList(),
        DCI(dri, kind),
        platforms,
        styles,
        extras
    )

    fun header(level: Int, block: ContentBuilder.() -> Unit) {
        contents += ContentHeader(level, group(ContentKind.Symbol, block))
    }

    private fun createText(text: String) =
        ContentText(text, DCI(dri, ContentKind.Symbol), platforms, styles, extras)

    fun text(text: String) {
        contents += createText(text)
    }

    inline fun <T : Any> block(
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
            elements.map { group(platforms, dri, kind) { operation(it) } },
            DCI(dri, kind),
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
            DCI(dri, ContentKind.Symbol),
            platforms
        )
    }

    fun <T: DocumentationNode<*>> table(
        name: String,
        level: Int,
        kind: Kind,
        elements: Iterable<T>,
        operation: ContentBuilder.(T) -> Unit
    ) {
        header(level) { text(name) }

        contents += ContentTable(
            emptyList(),
            elements.map { row(it) }.toList(),
            DCI(dri, kind),
            platforms, emptySet(), emptySet()
        )
    }


    fun row(element: DocumentationNode<*>) =
        ContentGroup(
            listOf(
                ContentDRILink(
                    listOf(
                        ContentText(
                            element.descriptors.first().name.toString(),
                            DCI(dri, XmlTransformer.XMLKind.Main),
                            platforms, emptySet(), emptySet()
                        )
                    ),
                    element.dri,
                    DCI(element.dri, XmlTransformer.XMLKind.XmlList),
                    element.platformData.toSet(), emptySet(), emptySet()
                ),
                ContentText(
                    element.briefDocstring,
                    DCI(element.dri, XmlTransformer.XMLKind.XmlList),
                    element.platformData.toSet(), emptySet(), emptySet()
                )
            ),
            DCI(dri, XmlTransformer.XMLKind.XmlList),
            platforms, emptySet(), emptySet()
        )


    private inline fun group(kind: Kind, block: ContentBuilder.() -> Unit): ContentGroup =
        group(platforms, dri, kind, block)
}

internal inline fun group(
    platforms: Set<PlatformData>,
    dri: DRI,
    kind: Kind = ContentKind.Main,
    block: ContentBuilder.() -> Unit
) = ContentBuilder(platforms, dri, kind).apply(block).build()


