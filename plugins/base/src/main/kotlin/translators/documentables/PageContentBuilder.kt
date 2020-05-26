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
        DocumentableContentBuilder(setOf(dri), sourceSets, styles, extra)
            .apply(block)
            .build(sourceSets, kind, styles, extra)

    fun contentFor(
        dri: Set<DRI>,
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
        DocumentableContentBuilder(setOf(d.dri), sourceSets, styles, extra)
            .apply(block)
            .build(sourceSets, kind, styles, extra)

    @ContentBuilderMarker
    open inner class DocumentableContentBuilder(
        val mainDRI: Set<DRI>,
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
            DCI(mainDRI, kind),
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
            text: String,
            kind: Kind = ContentKind.Main,
            platformData: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit = {}
        ) {
            contents += ContentHeader(
                level,
                contentFor(
                    mainDRI,
                    platformData,
                    kind,
                    styles,
                    extra + SimpleAttr("anchor", text.replace("\\s".toRegex(), "").toLowerCase())
                ) {
                    text(text)
                    block()
                }
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
                DCI(mainDRI, kind),
                sourceSets, styles, extra
            )
        }

        fun table(
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: DocumentableContentBuilder.() -> List<ContentGroup>
        ) {
            contents += ContentTable(
                emptyList(),
                operation(),
                DCI(mainDRI, kind),
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
                header(level, name) { }
                contents += ContentTable(
                    emptyList(),
                    elements.map {
                        buildGroup(setOf(it.dri), it.sourceSets.toSet(), kind, styles, extra) {
                            operation(it)
                        }
                    },
                    DCI(mainDRI, kind),
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
            contents += linkNode(text, address, kind, sourceSets, styles, extra)
        }

        fun linkNode(
            text: String,
            address: DRI,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) = ContentDRILink(
            listOf(createText(text, kind, sourceSets, styles, extra)),
            address,
            DCI(mainDRI, kind),
            sourceSets
        )

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
                dci = DCI(mainDRI, kind),
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
                DCI(mainDRI, kind),
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
                DCI(mainDRI, kind),
                sourceSets
            )
            contents += ContentGroup(content, DCI(mainDRI, kind), sourceSets, styles, extra)
        }

        fun group(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += buildGroup(dri, sourceSets, kind, styles, extra, block)
        }

        fun divergentGroup(
            groupID: ContentDivergentGroup.GroupID,
            dri: Set<DRI> = mainDRI,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            implicitlySourceSetHinted: Boolean = true,
            block: DivergentBuilder.() -> Unit
        ) {
            contents +=
                DivergentBuilder(dri, kind, styles, extra)
                    .apply(block)
                    .build(groupID = groupID, implicitlySourceSetHinted = implicitlySourceSetHinted)
        }

        fun buildGroup(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<SourceSetData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ): ContentGroup = contentFor(dri, sourceSets, kind, styles, extra, block)

        fun sourceSetDependentHint(
            dri: Set<DRI> = mainDRI,
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

        fun sourceSetDependentHint(
            dri: DRI,
            platformData: Set<SourceSetData> = mainPlatformData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += PlatformHintedContent(
                buildGroup(setOf(dri), platformData, kind, styles, extra, block),
                platformData
            )
        }

        protected fun createText(
            text: String,
            kind: Kind,
            sourceSets: Set<SourceSetData>,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ) =
            ContentText(text, DCI(mainDRI, kind), sourceSets, styles, extra)

        fun <T> platformText(
            value: SourceSetDependent<T>,
            platforms: Set<SourceSetData> = value.keys,
            transform: (T) -> String
        ) = value.entries.filter { it.key in platforms }.mapNotNull { (p, v) ->
            transform(v).takeIf { it.isNotBlank() }?.let { it to p }
        }.groupBy({ it.first }) { it.second }.forEach {
            text(it.key, sourceSets = it.value.toSet())
        }
    }

    @ContentBuilderMarker
    open inner class DivergentBuilder(
        private val mainDRI: Set<DRI>,
        private val mainKind: Kind,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private val instances: MutableList<ContentDivergentInstance> = mutableListOf()
        fun instance(
            dri: Set<DRI>,
            sourceSets: Set<SourceSetData>,  // Having correct PlatformData is crucial here, that's why there's no default
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DivergentInstanceBuilder.() -> Unit
        ) {
            instances += DivergentInstanceBuilder(dri, sourceSets, styles, extra)
                .apply(block)
                .build(kind)
        }

        fun build(
            groupID: ContentDivergentGroup.GroupID,
            implicitlySourceSetHinted: Boolean,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) = ContentDivergentGroup(
            instances.toList(),
            DCI(mainDRI, kind),
            styles,
            extra,
            groupID,
            implicitlySourceSetHinted
        )
    }

    @ContentBuilderMarker
    open inner class DivergentInstanceBuilder(
        private val mainDRI: Set<DRI>,
        private val mainSourceSets: Set<SourceSetData>,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private var before: ContentNode? = null
        private var divergent: ContentNode? = null
        private var after: ContentNode? = null

        fun before(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<SourceSetData> = mainSourceSets,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contentFor(dri, sourceSets, kind, styles, extra, block)
                .takeIf { it.children.isNotEmpty() }
                .also { before = it }
        }

        fun divergent(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<SourceSetData> = mainSourceSets,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            divergent = contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        fun after(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<SourceSetData> = mainSourceSets,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contentFor(dri, sourceSets, kind, styles, extra, block)
                .takeIf { it.children.isNotEmpty() }
                .also { after = it }
        }


        fun build(
            kind: Kind,
            sourceSets: Set<SourceSetData> = mainSourceSets,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) =
            ContentDivergentInstance(
                before,
                divergent!!,
                after,
                DCI(mainDRI, kind),
                sourceSets,
                styles,
                extra
            )
    }
}