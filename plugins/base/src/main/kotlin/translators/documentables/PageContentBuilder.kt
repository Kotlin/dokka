package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.TypeWrapper
import org.jetbrains.dokka.model.doc.DocTag
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
        extras: Set<Extra> = emptySet(),
        block: DocumentableContentBuilder.() -> Unit
    ): ContentGroup =
        DocumentableContentBuilder(dri, platformData, styles, extras)
            .apply(block)
            .build(platformData, kind, styles, extras)

    fun contentFor(
        d: Documentable,
        kind: Kind = ContentKind.Main,
        styles: Set<Style> = emptySet(),
        extras: Set<Extra> = emptySet(),
        block: DocumentableContentBuilder.() -> Unit
    ): ContentGroup =
        DocumentableContentBuilder(d.dri, d.platformData.toSet(), styles, extras)
            .apply(block)
            .build(d.platformData.toSet(), kind, styles, extras)

    @ContentBuilderMarker
    open inner class DocumentableContentBuilder(
        val mainDRI: DRI,
        val mainPlatformData: Set<PlatformData>,
        val mainStyles: Set<Style>,
        val mainExtras: Set<Extra>
    ) {
        protected val contents = mutableListOf<ContentNode>()

        fun build(
            platformData: Set<PlatformData>,
            kind: Kind,
            styles: Set<Style>,
            extras: Set<Extra>
        ) = ContentGroup(
            contents.toList(),
            DCI(setOf(mainDRI), kind),
            platformData,
            styles,
            extras
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
            extras: Set<Extra> = mainExtras,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentHeader(
                level,
                contentFor(mainDRI, mainPlatformData, kind, styles, extras, block)
            )
        }

        fun text(
            text: String,
            kind: Kind = ContentKind.Main,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extras: Set<Extra> = mainExtras
        ) {
            contents += createText(text, kind, platformData, styles, extras)
        }

        fun signature(d: Documentable) = ContentGroup(
            signatureProvider.signature(d),
            DCI(setOf(d.dri), ContentKind.Symbol),
            d.platformData.toSet(),
            mainStyles,
            mainExtras
        )

        fun linkTable(
            elements: List<DRI>,
            kind: Kind = ContentKind.Main,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extras: Set<Extra> = mainExtras
        ) {
            contents += ContentTable(
                emptyList(),
                elements.map {
                    contentFor(it, platformData, kind, styles, extras) {
                        link(it.classNames ?: "", it)
                    }
                },
                DCI(setOf(mainDRI), kind),
                platformData, styles, extras
            )
        }

        fun <T : Documentable> block(
            name: String,
            level: Int,
            kind: Kind = ContentKind.Main,
            elements: Iterable<T>,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extras: Set<Extra> = mainExtras,
            renderWhenEmpty: Boolean = false,
            operation: DocumentableContentBuilder.(T) -> Unit
        ) {
            if (renderWhenEmpty || elements.any()) {
                header(level) { text(name) }
                contents += ContentTable(
                    emptyList(),
                    elements.map {
                        buildGroup(it.dri, it.platformData.toSet(), kind, styles, extras) {
                            // TODO this will fail
                            operation(it)
                        }
                    },
                    DCI(setOf(mainDRI), kind),
                    platformData, styles, extras
                )
            }
        }

        fun <T> list(
            elements: List<T>,
            prefix: String = "",
            suffix: String = "",
            separator: String = ", ",
            operation: DocumentableContentBuilder.(T) -> Unit
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

        fun link(
            text: String,
            address: DRI,
            kind: Kind = ContentKind.Main,
            platformData: Set<PlatformData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extras: Set<Extra> = mainExtras
        ) {
            contents += ContentDRILink(
                listOf(createText(text, kind, platformData, styles, extras)),
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
            extras: Set<Extra> = mainExtras,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentDRILink(
                contentFor(mainDRI, platformData, kind, styles, extras, block).children,
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
            extras: Set<Extra> = mainExtras
        ) {
            val content = commentsConverter.buildContent(
                docTag,
                DCI(setOf(mainDRI), kind),
                platformData
            )
            contents += ContentGroup(content, DCI(setOf(mainDRI), kind), platformData, styles, extras)
        }

        fun group(
            dri: DRI = mainDRI,
            platformData: Set<PlatformData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extras: Set<Extra> = mainExtras,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += buildGroup(dri, platformData, kind, styles, extras, block)
        }

        fun buildGroup(
            dri: DRI = mainDRI,
            platformData: Set<PlatformData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extras: Set<Extra> = mainExtras,
            block: DocumentableContentBuilder.() -> Unit
        ): ContentGroup = contentFor(dri, platformData, kind, styles, extras, block)

        fun platformDependentHint(
            dri: DRI = mainDRI,
            platformData: Set<PlatformData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extras: Set<Extra> = mainExtras,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += PlatformHintedContent(
                buildGroup(dri, platformData, kind, styles, extras, block),
                platformData
            )
        }

        protected fun createText(
            text: String,
            kind: Kind,
            platformData: Set<PlatformData>,
            styles: Set<Style>,
            extras: Set<Extra>
        ) =
            ContentText(text, DCI(setOf(mainDRI), kind), platformData, styles, extras)

        fun type(t: TypeWrapper) {
            if (t.constructorNamePathSegments.isNotEmpty() && t.dri != null)
                link(t.constructorNamePathSegments.last(), t.dri!!)
            else if (t.constructorNamePathSegments.isNotEmpty() && t.dri == null)
                text(t.toString())
            else {
                logger.error("type $t cannot be resolved")
                text("???")
            }
            list(t.arguments, prefix = "<", suffix = ">") {
                type(it)
            }
        }
    }
}