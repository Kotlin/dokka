package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.plus
import org.jetbrains.dokka.model.toDisplaySourceSets
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
        sourceSets: Set<DokkaSourceSet>,
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
        sourceSets: Set<DokkaSourceSet>,
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
        sourceSets: Set<DokkaSourceSet> = d.sourceSets.toSet(),
        block: DocumentableContentBuilder.() -> Unit = {}
    ): ContentGroup =
        DocumentableContentBuilder(setOf(d.dri), sourceSets, styles, extra)
            .apply(block)
            .build(sourceSets, kind, styles, extra)

    @ContentBuilderMarker
    open inner class DocumentableContentBuilder(
        val mainDRI: Set<DRI>,
        val mainSourcesetData: Set<DokkaSourceSet>,
        val mainStyles: Set<Style>,
        val mainExtra: PropertyContainer<ContentNode>
    ) {
        protected val contents = mutableListOf<ContentNode>()

        fun build(
            sourceSets: Set<DokkaSourceSet>,
            kind: Kind,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ) = ContentGroup(
            contents.toList(),
            DCI(mainDRI, kind),
            sourceSets.toDisplaySourceSets(),
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
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit = {}
        ) {
            contents += ContentHeader(
                level,
                contentFor(
                    mainDRI,
                    sourceSets,
                    kind,
                    styles,
                    extra + SymbolAnchorHint(text.replace("\\s".toRegex(), "").toLowerCase(), kind)
                ) {
                    text(text, kind = kind)
                    block()
                }
            )
        }

        fun cover(
            text: String,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles + TextStyle.Cover,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit = {}
        ) {
            header(1, text, sourceSets = sourceSets, styles = styles, extra = extra, block = block)
        }

        fun constant(text: String) = text(text, styles = mainStyles + TokenStyle.Constant)
        fun keyword(text: String) = text(text, styles = mainStyles + TokenStyle.Keyword)
        fun stringLiteral(text: String) = text(text, styles = mainStyles + TokenStyle.String)
        fun booleanLiteral(value: Boolean) = text(value.toString(), styles = mainStyles + TokenStyle.Boolean)
        fun punctuation(text: String) = text(text, styles = mainStyles + TokenStyle.Punctuation)
        fun operator(text: String) = text(text, styles = mainStyles + TokenStyle.Operator)

        fun text(
            text: String,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += createText(text, kind, sourceSets, styles, extra)
        }

        fun breakLine(sourceSets: Set<DokkaSourceSet> = mainSourcesetData) {
            contents += ContentBreakLine(sourceSets.toDisplaySourceSets())
        }

        fun buildSignature(d: Documentable) = signatureProvider.signature(d)

        fun table(
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: TableBuilder.() -> Unit = {}
        ) {
            contents += TableBuilder(mainDRI, sourceSets, kind, styles, extra).apply {
                operation()
            }.build()
        }

        fun unorderedList(
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: ListBuilder.() -> Unit = {}
        ) {
            contents += ListBuilder(false, mainDRI, sourceSets, kind, styles, extra).apply(operation).build()
        }

        fun orderedList(
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: ListBuilder.() -> Unit = {}
        ) {
            contents += ListBuilder(true, mainDRI, sourceSets, kind, styles, extra).apply(operation).build()
        }

        fun descriptionList(
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: ListBuilder.() -> Unit = {}
        ) {
            contents += ListBuilder(false, mainDRI, sourceSets, kind, styles + ListStyle.DescriptionList, extra)
                .apply(operation)
                .build()
        }

        internal fun headers(vararg label: String) = contentFor(mainDRI, mainSourcesetData) {
            label.forEach { text(it) }
        }

        fun <T : Documentable> block(
            name: String,
            level: Int,
            kind: Kind = ContentKind.Main,
            elements: Iterable<T>,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            renderWhenEmpty: Boolean = false,
            needsSorting: Boolean = true,
            headers: List<ContentGroup> = emptyList(),
            needsAnchors: Boolean = false,
            operation: DocumentableContentBuilder.(T) -> Unit
        ) {
            if (renderWhenEmpty || elements.any()) {
                header(level, name, kind = kind) { }
                contents += ContentTable(
                    header = headers,
                    children = elements
                        .let {
                            if (needsSorting)
                                it.sortedWith(compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it.name })
                            else it
                        }
                        .map {
                            val newExtra = if (needsAnchors) extra + SymbolAnchorHint.from(it, kind) else extra
                            buildGroup(setOf(it.dri), it.sourceSets.toSet(), kind, styles, newExtra) {
                                operation(it)
                            }
                        },
                    dci = DCI(mainDRI, kind),
                    sourceSets = sourceSets.toDisplaySourceSets(),
                    style = styles,
                    extra = extra
                )
            }
        }

        fun <T : Pair<String, List<Documentable>>> multiBlock(
            name: String,
            level: Int,
            kind: Kind = ContentKind.Main,
            groupedElements: Iterable<T>,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            renderWhenEmpty: Boolean = false,
            needsSorting: Boolean = true,
            headers: List<ContentGroup> = emptyList(),
            needsAnchors: Boolean = false,
            extraHead: PropertyContainer<ContentNode> = mainExtra,
            operation: DocumentableContentBuilder.(String, List<Documentable>) -> Unit
        ) {
            if (renderWhenEmpty || groupedElements.any()) {
                header(level, name, kind = kind, extra = extraHead) { }
                contents += ContentTable(
                    header = headers,
                    children = groupedElements
                        .let {
                            if (needsSorting)
                                it.sortedWith(compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it.first })
                            else it
                        }
                        .map {
                            val documentables = it.second
                            val newExtraWithTab = if (documentables.all { it.isExtension() }) mainExtra + SimpleAttr.togglableTarget("Extensions") else mainExtra
                            val newExtra = if (needsAnchors) newExtraWithTab + SymbolAnchorHint(it.first, kind) else newExtraWithTab
                            buildGroup(
                                documentables.map { it.dri }.toSet(),
                                documentables.flatMap { it.sourceSets }.toSet(),
                                kind,
                                styles,
                                newExtra
                            ) {
                                operation(it.first, documentables)
                            }
                        },
                    dci = DCI(mainDRI, kind),
                    sourceSets = sourceSets.toDisplaySourceSets(),
                    style = styles,
                    extra = extra
                )
            }
        }

        fun <T> list(
            elements: List<T>,
            prefix: String = "",
            suffix: String = "",
            separator: String = ", ",
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData, // TODO: children should be aware of this platform data
            surroundingCharactersStyle: Set<Style> = mainStyles,
            separatorStyles: Set<Style> = mainStyles,
            operation: DocumentableContentBuilder.(T) -> Unit
        ) {
            if (elements.isNotEmpty()) {
                if (prefix.isNotEmpty()) text(prefix, sourceSets = sourceSets, styles = surroundingCharactersStyle)
                elements.dropLast(1).forEach {
                    operation(it)
                    text(separator, sourceSets = sourceSets, styles = separatorStyles)
                }
                operation(elements.last())
                if (suffix.isNotEmpty()) text(suffix, sourceSets = sourceSets, styles = surroundingCharactersStyle)
            }
        }

        fun link(
            text: String,
            address: DRI,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += linkNode(text, address, DCI(mainDRI, kind), sourceSets, styles, extra)
        }

        fun linkNode(
            text: String,
            address: DRI,
            dci: DCI = DCI(mainDRI, ContentKind.Main),
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) = ContentDRILink(
            listOf(createText(text, dci.kind, sourceSets, styles, extra)),
            address,
            dci,
            sourceSets.toDisplaySourceSets(),
            extra = extra
        )

        fun link(
            text: String,
            address: String,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += ContentResolvedLink(
                children = listOf(createText(text, kind, sourceSets, styles, extra)),
                address = address,
                extra = PropertyContainer.empty(),
                dci = DCI(mainDRI, kind),
                sourceSets = sourceSets.toDisplaySourceSets(),
                style = emptySet()
            )
        }

        fun link(
            address: DRI,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentDRILink(
                contentFor(mainDRI, sourceSets, kind, styles, extra, block).children,
                address,
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                extra = extra
            )
        }

        fun comment(
            docTag: DocTag,
            kind: Kind = ContentKind.Comment,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            val content = commentsConverter.buildContent(
                docTag,
                DCI(mainDRI, kind),
                sourceSets
            )
            contents += ContentGroup(content, DCI(mainDRI, kind), sourceSets.toDisplaySourceSets(), styles, extra)
        }

        fun codeBlock(
            language: String = "",
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentCodeBlock(
                contentFor(mainDRI, sourceSets, kind, styles, extra, block).children,
                language,
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                styles,
                extra
            )
        }

        fun codeInline(
            language: String = "",
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += ContentCodeInline(
                contentFor(mainDRI, sourceSets, kind, styles, extra, block).children,
                language,
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                styles,
                extra
            )
        }

        fun firstParagraphComment(
            content: DocTag,
            kind: Kind = ContentKind.Comment,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            firstParagraphBrief(content)?.let { brief ->
                val builtDescription = commentsConverter.buildContent(
                    brief,
                    DCI(mainDRI, kind),
                    sourceSets
                )

                contents += ContentGroup(
                    builtDescription,
                    DCI(mainDRI, kind),
                    sourceSets.toDisplaySourceSets(),
                    styles,
                    extra
                )
            }
        }

        fun firstSentenceComment(
            content: DocTag,
            kind: Kind = ContentKind.Comment,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ){
            val builtDescription = commentsConverter.buildContent(
                content,
                DCI(mainDRI, kind),
                sourceSets
            )

            contents += ContentGroup(
                firstSentenceBriefFromContentNodes(builtDescription),
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                styles,
                extra
            )
        }

        fun group(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
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
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ): ContentGroup = contentFor(dri, sourceSets, kind, styles, extra, block)

        fun sourceSetDependentHint(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += PlatformHintedContent(
                buildGroup(dri, sourceSets, kind, styles, extra, block),
                sourceSets.toDisplaySourceSets()
            )
        }

        fun sourceSetDependentHint(
            dri: DRI,
            sourcesetData: Set<DokkaSourceSet> = mainSourcesetData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += PlatformHintedContent(
                buildGroup(setOf(dri), sourcesetData, kind, styles, extra, block),
                sourcesetData.toDisplaySourceSets()
            )
        }

        protected fun createText(
            text: String,
            kind: Kind,
            sourceSets: Set<DokkaSourceSet>,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ) =
            ContentText(text, DCI(mainDRI, kind), sourceSets.toDisplaySourceSets(), styles, extra)

        fun <T> sourceSetDependentText(
            value: SourceSetDependent<T>,
            sourceSets: Set<DokkaSourceSet> = value.keys,
            styles: Set<Style> = mainStyles,
            transform: (T) -> String
        ) = value.entries.filter { it.key in sourceSets }.mapNotNull { (p, v) ->
            transform(v).takeIf { it.isNotBlank() }?.let { it to p }
        }.groupBy({ it.first }) { it.second }.forEach {
            text(it.key, sourceSets = it.value.toSet(), styles = styles)
        }
    }

    @ContentBuilderMarker
    open inner class TableBuilder(
        private val mainDRI: Set<DRI>,
        private val mainSourceSets: Set<DokkaSourceSet>,
        private val mainKind: Kind,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private val headerRows: MutableList<ContentGroup> = mutableListOf()
        private val rows: MutableList<ContentGroup> = mutableListOf()
        private var caption: ContentGroup? = null

        fun header(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            headerRows += contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        fun row(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            rows += contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        fun caption(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            caption = contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        fun build(
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) = ContentTable(
            headerRows,
            caption,
            rows,
            DCI(mainDRI, kind),
            sourceSets.toDisplaySourceSets(),
            styles, extra
        )
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
            sourceSets: Set<DokkaSourceSet>,  // Having correct sourcesetData is crucial here, that's why there's no default
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
        private val mainSourceSets: Set<DokkaSourceSet>,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private var before: ContentNode? = null
        private var divergent: ContentNode? = null
        private var after: ContentNode? = null

        fun before(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contentFor(dri, sourceSets, kind, styles, extra, block)
                .takeIf { it.hasAnyContent() }
                .also { before = it }
        }

        fun divergent(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            divergent = contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        fun after(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contentFor(dri, sourceSets, kind, styles, extra, block)
                .takeIf { it.hasAnyContent() }
                .also { after = it }
        }


        fun build(
            kind: Kind,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) =
            ContentDivergentInstance(
                before,
                divergent ?: throw IllegalStateException("Divergent block needs divergent part"),
                after,
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                styles,
                extra
            )
    }

    @ContentBuilderMarker
    open inner class ListBuilder(
        val ordered: Boolean,
        private val mainDRI: Set<DRI>,
        private val mainSourceSets: Set<DokkaSourceSet>,
        private val mainKind: Kind,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private val contentNodes: MutableList<ContentNode> = mutableListOf()

        fun item(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contentNodes += contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        fun build(
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) = ContentList(
            contentNodes,
            ordered,
            DCI(mainDRI, kind),
            sourceSets.toDisplaySourceSets(),
            styles, extra
        )
    }
}
