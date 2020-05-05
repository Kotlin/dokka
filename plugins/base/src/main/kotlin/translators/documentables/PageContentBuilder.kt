package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

@DslMarker
annotation class ContentBuilderMarker

open class PageContentBuilder(
    val commentsConverter: CommentsToContentConverter,
    val signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    fun contentFor(
        dri: DRI,
        sourceSets: Set<SourceSetData>,
        kind: Kind = ContentKind.Main,
        styles: Set<Style> = emptySet(),
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        block: DocumentableContentBuilder.() -> Unit
    ): ContentGroup =
        DocumentableContentBuilder(dri, sourceSets, styles, extra)
            .apply(block)
            .build(sourceSets, kind, styles, extra)

    fun contentFor(
        d: Documentable,
        kind: Kind = ContentKind.Main,
        styles: Set<Style> = emptySet(),
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        sourceSets: Set<SourceSetData> = d.sourceSets.toSet(),
        block: DocumentableContentBuilder.() -> Unit = {}
    ): ContentGroup =
        DocumentableContentBuilder(d.dri, sourceSets, styles, extra)
            .apply(block)
            .build(sourceSets, kind, styles, extra)

    @ContentBuilderMarker
    open inner class DocumentableContentBuilder(
        val mainDRI: DRI,
        val mainPlatformData: Set<SourceSetData>,
        val mainStyles: Set<Style>,
        val mainExtra: PropertyContainer<ContentNode>
    ) {
        protected val contents = mutableListOf<ContentNode>()

        fun build(
            sourceSets: Set<SourceSetData>,
            kind: Kind,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ) = ContentGroup(
            contents.toList(),
            DCI(setOf(mainDRI), kind),
            sourceSets,
            styles,
            extra
        )

        operator fun ContentNode.unaryPlus() {
            contents += this
        }

        operator fun Collection<ContentNode>.unaryPlus() {
            contents += this
        }

        fun header(
            level: Int,
            kind: Kind = ContentKind.Main,
            platformData: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentHeader(
                level,
                contentFor(mainDRI, platformData, kind, styles, extra, block)
            )
        }

        fun text(
            text: String,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += createText(text, kind, sourceSets, styles, extra)
        }

        fun buildSignature(d: Documentable) = signatureProvider.signature(d)

        fun linkTable(
            elements: List<DRI>,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += ContentTable(
                emptyList(),
                elements.map {
                    contentFor(it, sourceSets, kind, styles, extra) {
                        link(it.classNames ?: "", it)
                    }
                },
                DCI(setOf(mainDRI), kind),
                sourceSets, styles, extra
            )
        }

        fun table(
            dri: DRI = mainDRI,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: DocumentableContentBuilder.() -> List<ContentGroup>
        ) {
            contents += ContentTable(
                emptyList(),
                operation(),
                DCI(setOf(mainDRI), kind),
                sourceSets, styles, extra
            )
        }

        fun <T : Documentable> block(
            name: String,
            level: Int,
            kind: Kind = ContentKind.Main,
            elements: Iterable<T>,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            renderWhenEmpty: Boolean = false,
            operation: DocumentableContentBuilder.(T) -> Unit
        ) {
            if (renderWhenEmpty || elements.any()) {
                header(level) { text(name) }
                contents += ContentTable(
                    emptyList(),
                    elements.map {
                        buildGroup(it.dri, it.sourceSets.toSet(), kind, styles, extra) {
                            operation(it)
                        }
                    },
                    DCI(setOf(mainDRI), kind),
                    sourceSets, styles, extra
                )
            }
        }

        fun <T> list(
            elements: List<T>,
            prefix: String = "",
            suffix: String = "",
            separator: String = ", ",
            sourceSets: Set<SourceSetData> = mainPlatformData, // TODO: children should be aware of this platform data
            operation: DocumentableContentBuilder.(T) -> Unit
        ) {
            if (elements.isNotEmpty()) {
                if (prefix.isNotEmpty()) text(prefix, sourceSets = sourceSets)
                elements.dropLast(1).forEach {
                    operation(it)
                    text(separator, sourceSets = sourceSets)
                }
                operation(elements.last())
                if (suffix.isNotEmpty()) text(suffix, sourceSets = sourceSets)
            }
        }

        fun link(
            text: String,
            address: DRI,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += ContentDRILink(
                listOf(createText(text, kind, sourceSets, styles, extra)),
                address,
                DCI(setOf(mainDRI), kind),
                sourceSets
            )
        }

        fun link(
            text: String,
            address: String,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) =
        ContentResolvedLink(
            children = listOf(createText(text, kind, sourceSets, styles, extra)),
            address = address,
            extra = PropertyContainer.empty(),
            dci = DCI(setOf(mainDRI), kind),
            sourceSets = sourceSets,
            style = emptySet()
        )

        fun link(
            address: DRI,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentDRILink(
                contentFor(mainDRI, sourceSets, kind, styles, extra, block).children,
                address,
                DCI(setOf(mainDRI), kind),
                sourceSets
            )
        }

        fun comment(
            docTag: DocTag,
            kind: Kind = ContentKind.Comment,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            val content = commentsConverter.buildContent(
                docTag,
                DCI(setOf(mainDRI), kind),
                sourceSets
            )
            contents += ContentGroup(content, DCI(setOf(mainDRI), kind), sourceSets, styles, extra)
        }

        fun group(
            dri: DRI = mainDRI,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += buildGroup(dri, sourceSets, kind, styles, extra, block)
        }

        fun buildGroup(
            dri: DRI = mainDRI,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ): ContentGroup = contentFor(dri, sourceSets, kind, styles, extra, block)

        fun sourceSetDependentHint(
            dri: DRI = mainDRI,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += PlatformHintedContent(
                buildGroup(dri, sourceSets, kind, styles, extra, block),
                sourceSets
            )
        }

        protected fun createText(
            text: String,
            kind: Kind,
            sourceSets: Set<SourceSetData>,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ) =
            ContentText(text, DCI(setOf(mainDRI), kind), sourceSets, styles, extra)

        fun <T> platformText(
            value: SourceSetDependent<T>,
            platforms: Set<SourceSetData> = value.keys,
            transform: (T) -> String
        ) = value.entries.filter { it.key in platforms }.forEach { (p, v) ->
            transform(v).takeIf { it.isNotBlank() }?.also { text(it, sourceSets = setOf(p)) }
        }
    }
}