/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
public annotation class ContentBuilderMarker

public open class PageContentBuilder(
    public val commentsConverter: CommentsToContentConverter,
    public val signatureProvider: SignatureProvider,
    public val logger: DokkaLogger
) {
    public fun contentFor(
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

    public fun contentFor(
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

    public fun contentFor(
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
    public open inner class DocumentableContentBuilder(
        public val mainDRI: Set<DRI>,
        public val mainSourcesetData: Set<DokkaSourceSet>,
        public val mainStyles: Set<Style>,
        public val mainExtra: PropertyContainer<ContentNode>
    ) {
        protected val contents: MutableList<ContentNode> = mutableListOf<ContentNode>()

        public fun build(
            sourceSets: Set<DokkaSourceSet>,
            kind: Kind,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>
        ): ContentGroup {
            return ContentGroup(
                children = contents.toList(),
                dci = DCI(mainDRI, kind),
                sourceSets = sourceSets.toDisplaySourceSets(),
                style = styles,
                extra = extra
            )
        }

        public operator fun ContentNode.unaryPlus() {
            contents += this
        }

        public operator fun Collection<ContentNode>.unaryPlus() {
            contents += this
        }

        public fun header(
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

        public fun cover(
            text: String,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles + TextStyle.Cover,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit = {}
        ) {
            header(1, text, sourceSets = sourceSets, styles = styles, extra = extra, block = block)
        }

        public fun constant(text: String) {
            text(text, styles = mainStyles + TokenStyle.Constant)
        }

        public fun keyword(text: String) {
            text(text, styles = mainStyles + TokenStyle.Keyword)
        }

        public fun stringLiteral(text: String) {
            text(text, styles = mainStyles + TokenStyle.String)
        }

        public fun booleanLiteral(value: Boolean) {
            text(value.toString(), styles = mainStyles + TokenStyle.Boolean)
        }

        public fun punctuation(text: String) {
            text(text, styles = mainStyles + TokenStyle.Punctuation)
        }

        public fun operator(text: String) {
            text(text, styles = mainStyles + TokenStyle.Operator)
        }

        public fun text(
            text: String,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += createText(text, kind, sourceSets, styles, extra)
        }

        public fun breakLine(sourceSets: Set<DokkaSourceSet> = mainSourcesetData) {
            contents += ContentBreakLine(sourceSets.toDisplaySourceSets())
        }

        public fun buildSignature(d: Documentable): List<ContentNode> = signatureProvider.signature(d)

        public fun table(
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

        public fun unorderedList(
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: ListBuilder.() -> Unit = {}
        ) {
            contents += ListBuilder(false, mainDRI, sourceSets, kind, styles, extra).apply(operation).build()
        }

        public fun orderedList(
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            operation: ListBuilder.() -> Unit = {}
        ) {
            contents += ListBuilder(true, mainDRI, sourceSets, kind, styles, extra).apply(operation).build()
        }

        public fun descriptionList(
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

        public fun <T : Documentable> block(
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

        public fun <T : Pair<String, List<Documentable>>> multiBlock(
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
            operation: DocumentableContentBuilder.(String, List<Documentable>) -> Unit
        ) {

            if (renderWhenEmpty || groupedElements.any()) {
                group(extra = extra) {
                    header(level, name, kind = kind) { }
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
                                val newExtra = if (needsAnchors) extra + SymbolAnchorHint(
                                    it.first,
                                    kind
                                ) else extra
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
        }

        public fun <T> list(
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

        public fun link(
            text: String,
            address: DRI,
            kind: Kind = ContentKind.Main,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ) {
            contents += linkNode(text, address, DCI(mainDRI, kind), sourceSets, styles, extra)
        }

        public fun linkNode(
            text: String,
            address: DRI,
            dci: DCI = DCI(mainDRI, ContentKind.Main),
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ): ContentLink {
            return ContentDRILink(
                listOf(createText(text, dci.kind, sourceSets, styles, extra)),
                address,
                dci,
                sourceSets.toDisplaySourceSets(),
                extra = extra
            )
        }

        public fun link(
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

        public fun link(
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

        public fun comment(
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

        public fun codeBlock(
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

        public fun codeInline(
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

        public fun firstParagraphComment(
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

        public fun firstSentenceComment(
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

        public fun group(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contents += buildGroup(dri, sourceSets, kind, styles, extra, block)
        }

        public fun divergentGroup(
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

        public fun buildGroup(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourcesetData,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ): ContentGroup = contentFor(dri, sourceSets, kind, styles, extra, block)

        public fun sourceSetDependentHint(
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

        public fun sourceSetDependentHint(
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
        ): ContentText {
            return ContentText(text, DCI(mainDRI, kind), sourceSets.toDisplaySourceSets(), styles, extra)
        }

        public fun <T> sourceSetDependentText(
            value: SourceSetDependent<T>,
            sourceSets: Set<DokkaSourceSet> = value.keys,
            styles: Set<Style> = mainStyles,
            transform: (T) -> String
        ) {
            value.entries
                .filter { it.key in sourceSets }
                .mapNotNull { (p, v) -> transform(v).takeIf { it.isNotBlank() }?.let { it to p } }
                .groupBy({ it.first }) { it.second }
                    .forEach { text(it.key, sourceSets = it.value.toSet(), styles = styles) }
        }
    }

    @ContentBuilderMarker
    public open inner class TableBuilder(
        private val mainDRI: Set<DRI>,
        private val mainSourceSets: Set<DokkaSourceSet>,
        private val mainKind: Kind,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private val headerRows: MutableList<ContentGroup> = mutableListOf()
        private val rows: MutableList<ContentGroup> = mutableListOf()
        private var caption: ContentGroup? = null

        public fun header(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            headerRows += contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        public fun row(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            rows += contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        public fun caption(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            caption = contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        public fun build(
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ): ContentTable {
            return ContentTable(
                headerRows,
                caption,
                rows,
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                styles, extra
            )
        }
    }

    @ContentBuilderMarker
    public open inner class DivergentBuilder(
        private val mainDRI: Set<DRI>,
        private val mainKind: Kind,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private val instances: MutableList<ContentDivergentInstance> = mutableListOf()

        public fun instance(
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

        public fun build(
            groupID: ContentDivergentGroup.GroupID,
            implicitlySourceSetHinted: Boolean,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ): ContentDivergentGroup {
            return ContentDivergentGroup(
                children = instances.toList(),
                dci = DCI(mainDRI, kind),
                style = styles,
                extra = extra,
                groupID = groupID,
                implicitlySourceSetHinted = implicitlySourceSetHinted
            )
        }
    }

    @ContentBuilderMarker
    public open inner class DivergentInstanceBuilder(
        private val mainDRI: Set<DRI>,
        private val mainSourceSets: Set<DokkaSourceSet>,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private var before: ContentNode? = null
        private var divergent: ContentNode? = null
        private var after: ContentNode? = null

        public fun before(
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

        public fun divergent(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = ContentKind.Main,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            divergent = contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        public fun after(
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

        public fun build(
            kind: Kind,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ): ContentDivergentInstance {
            return ContentDivergentInstance(
                before,
                divergent ?: throw IllegalStateException("Divergent block needs divergent part"),
                after,
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                styles,
                extra
            )
        }
    }

    @ContentBuilderMarker
    public open inner class ListBuilder(
        public val ordered: Boolean,
        private val mainDRI: Set<DRI>,
        private val mainSourceSets: Set<DokkaSourceSet>,
        private val mainKind: Kind,
        private val mainStyles: Set<Style>,
        private val mainExtra: PropertyContainer<ContentNode>
    ) {
        private val contentNodes: MutableList<ContentNode> = mutableListOf()

        public fun item(
            dri: Set<DRI> = mainDRI,
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra,
            block: DocumentableContentBuilder.() -> Unit
        ) {
            contentNodes += contentFor(dri, sourceSets, kind, styles, extra, block)
        }

        public fun build(
            sourceSets: Set<DokkaSourceSet> = mainSourceSets,
            kind: Kind = mainKind,
            styles: Set<Style> = mainStyles,
            extra: PropertyContainer<ContentNode> = mainExtra
        ): ContentList {
            return ContentList(
                contentNodes,
                ordered,
                DCI(mainDRI, kind),
                sourceSets.toDisplaySourceSets(),
                styles, extra
            )
        }
    }
}
