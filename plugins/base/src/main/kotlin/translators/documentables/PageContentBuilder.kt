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
        platformData: Set<PlatformData>,
        kind: Kind = ContentKind.Main,
        styles: Set<Style> = emptySet(),
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        block: DocumentableContentBuilder.() -> Unit
    ): ContentGroup =
        DocumentableContentBuilder(dri, platformData, styles, extra)
            .apply(block)
            .build(platformData, kind, styles, extra)

    fun contentFor(
        d: Documentable,
        kind: Kind = ContentKind.Main,
        styles: Set<Style> = emptySet(),
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        block: DocumentableContentBuilder.() -> Unit = {}
    ): ContentGroup =
        DocumentableContentBuilder(d.dri, d.platformData.toSet(), styles, extra)
            .apply(block)
            .build(d.platformData.toSet(), kind, styles, extra)

    @ContentBuilderMarker
    open inner class DocumentableContentBuilder(
        val mainDRI: DRI,
        val mainPlatformData: Set<PlatformData>,
        val mainStyles: Set<Style>,
        val mainExtra: PropertyContainer<ContentNode>
    ) {
        protected val contents = mutableListOf<ContentNode>()

        fun build(
            platformData: Set<PlatformData>,
            kind: Kind,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ) = ContentGroup(
            contents.toList(),
            DCI(setOf(mainDRI), kind),
            platformData,
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
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentHeader(
                level,
                contentFor(mainDRI, mainPlatformData, kind, styles, extra, block)
            )
        }

        fun text(
            text: String,
            kind: Kind = ContentKind.Main,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += createText(text, kind, platformData, styles, extra)
        }

        fun buildSignature(d: Documentable) = signatureProvider.signature(d)

        fun linkTable(
            elements: List<DRI>,
            kind: Kind = ContentKind.Main,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += ContentTable(
                emptyList(),
                elements.map {
                    contentFor(it, platformData, kind, styles, extra) {
                        link(it.classNames ?: "", it)
                    }
                },
                DCI(setOf(mainDRI), kind),
                platformData, styles, extra
            )
        }

        fun <T : Documentable> block(
            name: String,
            level: Int,
            kind: Kind = ContentKind.Main,
            elements: Iterable<T>,
            platformData: Set<PlatformData> = mainPlatformData,
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
                        buildGroup(it.dri, it.platformData.toSet(), kind, styles, extra) {
                            // TODO this will fail
                            operation(it)
                        }
                    },
                    DCI(setOf(mainDRI), kind),
                    platformData, styles, extra
                )
            }
        }

        fun <T> list(
            elements: List<T>,
            prefix: String = "",
            suffix: String = "",
            separator: String = ", ",
            platformData: Set<PlatformData> = mainPlatformData, // TODO: children should be aware of this platform data
            operation: DocumentableContentBuilder.(T) -> Unit
        ) {
            if (elements.isNotEmpty()) {
                if (prefix.isNotEmpty()) text(prefix, platformData = platformData)
                elements.dropLast(1).forEach {
                    operation(it)
                    text(separator, platformData = platformData)
                }
                operation(elements.last())
                if (suffix.isNotEmpty()) text(suffix, platformData = platformData)
            }
        }

        fun link(
            text: String,
            address: DRI,
            kind: Kind = ContentKind.Main,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += ContentDRILink(
                listOf(createText(text, kind, platformData, styles, extra)),
                address,
                DCI(setOf(mainDRI), kind),
                platformData
            )
        }

        fun link(
            address: DRI,
            kind: Kind = ContentKind.Main,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentDRILink(
                contentFor(mainDRI, platformData, kind, styles, extra, block).children,
                address,
                DCI(setOf(mainDRI), kind),
                platformData
            )
        }

        fun comment(
            docTag: DocTag,
            kind: Kind = ContentKind.Comment,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            val content = commentsConverter.buildContent(
                docTag,
                DCI(setOf(mainDRI), kind),
                platformData
            )
            contents += ContentGroup(content, DCI(setOf(mainDRI), kind), platformData, styles, extra)
        }

        fun group(
            dri: DRI = mainDRI,
            platformData: Set<PlatformData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += buildGroup(dri, platformData, kind, styles, extra, block)
        }

        fun buildGroup(
            dri: DRI = mainDRI,
            platformData: Set<PlatformData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ): ContentGroup = contentFor(dri, platformData, kind, styles, extra, block)

        fun breakLine(platformData: Set<PlatformData> = mainPlatformData) {
            contents += ContentBreakLine(platformData)
        }

        fun platformDependentHint(
            dri: DRI = mainDRI,
            platformData: Set<PlatformData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += PlatformHintedContent(
                buildGroup(dri, platformData, kind, styles, extra, block),
                platformData
            )
        }

        protected fun createText(
            text: String,
            kind: Kind,
            platformData: Set<PlatformData>,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ) =
            ContentText(text, DCI(setOf(mainDRI), kind), platformData, styles, extra)

        fun <T> platformText(
            value: PlatformDependent<T>,
            transform: (T) -> String
        ) = value.entries.forEach { (p, v) ->
            transform(v).takeIf { it.isNotBlank() }?.also { text(it, platformData = setOf(p)) }
        }
    }
}