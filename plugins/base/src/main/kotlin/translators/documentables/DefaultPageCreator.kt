package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.documentables.CallableExtensions
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.transformers.documentables.ClashingDriIdentifier

private typealias GroupedTags = Map<KClass<out TagWrapper>, List<Pair<DokkaSourceSet?, TagWrapper>>>

private val specialTags: Set<KClass<out TagWrapper>> =
    setOf(Property::class, Description::class, Constructor::class, Receiver::class, Param::class, See::class)

open class DefaultPageCreator(
    configuration: DokkaBaseConfiguration?,
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    protected open val contentBuilder = PageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    protected val separateInheritedMembers =
        configuration?.separateInheritedMembers ?: DokkaBaseConfiguration.separateInheritedMembersDefault

    open fun pageForModule(m: DModule) =
        ModulePageNode(m.name.ifEmpty { "<root>" }, contentForModule(m), m, m.packages.map(::pageForPackage))

    open fun pageForPackage(p: DPackage): PackagePageNode = PackagePageNode(
        p.name, contentForPackage(p), setOf(p.dri), p,
        p.classlikes.renameClashingDocumentable().map(::pageForClasslike) +
                p.functions.renameClashingDocumentable()
                    .map(::pageForFunction) + p.properties.mapNotNull(::pageForProperty)
    )

    open fun pageForEnumEntry(e: DEnumEntry): ClasslikePageNode =
        ClasslikePageNode(
            e.nameAfterClash(), contentForEnumEntry(e), setOf(e.dri), e,
            e.classlikes.renameClashingDocumentable().map(::pageForClasslike) +
                    e.filteredFunctions.renameClashingDocumentable().map(::pageForFunction) +
                    e.properties.renameClashingDocumentable().mapNotNull(::pageForProperty)
        )

    open fun pageForClasslike(c: DClasslike): ClasslikePageNode {
        val constructors = if (c is WithConstructors) c.constructors else emptyList()

        return ClasslikePageNode(
            c.nameAfterClash(), contentForClasslike(c), setOf(c.dri), c,
            constructors.map(::pageForFunction) +
                    c.classlikes.renameClashingDocumentable().map(::pageForClasslike) +
                    c.filteredFunctions.renameClashingDocumentable().map(::pageForFunction) +
                    c.filteredProperties.renameClashingDocumentable().mapNotNull(::pageForProperty) +
                    if (c is DEnum) c.entries.map(::pageForEnumEntry) else emptyList()

        )
    }

    private fun <T> T.toClashedName() where T : Documentable, T : WithExtraProperties<T> =
        (extra[ClashingDriIdentifier]?.value?.joinToString(", ", "[", "]") { it.displayName } ?: "") + name.orEmpty()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Documentable> List<T>.renameClashingDocumentable(): List<T> =
        groupBy { it.dri }.values.flatMap { elements ->
            if (elements.size == 1) elements else elements.mapNotNull { element ->
                when (element) {
                    is DClass -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    is DObject -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    is DAnnotation -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    is DInterface -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    is DEnum -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    is DFunction -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    is DProperty -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    is DTypeAlias -> element.copy(extra = element.extra + DriClashAwareName(element.toClashedName()))
                    else -> null
                } as? T?
            }
        }

    open fun pageForFunction(f: DFunction) = MemberPageNode(f.nameAfterClash(), contentForFunction(f), setOf(f.dri), f)

    open fun pageForProperty(p: DProperty): MemberPageNode? =
        MemberPageNode(p.nameAfterClash(), contentForProperty(p), setOf(p.dri), p)

    private fun <T> T.isInherited(): Boolean where T : Documentable, T : WithExtraProperties<T> =
        sourceSets.all { sourceSet -> extra[InheritedMember]?.isInherited(sourceSet) == true }

    private val WithScope.filteredFunctions: List<DFunction>
        get() = functions.filterNot { it.isInherited() }

    private val WithScope.filteredProperties: List<DProperty>
        get() = properties.filterNot { it.isInherited() }

    private fun <T> Collection<T>.splitInherited(): Pair<List<T>, List<T>> where T : Documentable, T : WithExtraProperties<T> =
        partition { it.isInherited() }

    protected open fun contentForModule(m: DModule) = contentBuilder.contentFor(m) {
        group(kind = ContentKind.Cover) {
            cover(m.name)
            if (contentForDescription(m).isNotEmpty()) {
                sourceSetDependentHint(
                    m.dri,
                    m.sourceSets.toSet(),
                    kind = ContentKind.SourceSetDependentHint,
                    styles = setOf(TextStyle.UnderCoverText)
                ) {
                    +contentForDescription(m)
                }
            }
        }
        +contentForComments(m)

        block(
            "Packages",
            2,
            ContentKind.Packages,
            m.packages,
            m.sourceSets.toSet(),
            needsAnchors = true,
            headers = listOf(
                headers("Name")
            )
        ) {
            val documentations = it.sourceSets.map { platform ->
                it.descriptions[platform]?.also { it.root }
            }
            val haveSameContent =
                documentations.all { it?.root == documentations.firstOrNull()?.root && it?.root != null }

            link(it.name, it.dri)
            if (it.sourceSets.size == 1 || (documentations.isNotEmpty() && haveSameContent)) {
                documentations.first()?.let { firstSentenceComment(kind = ContentKind.Comment, content = it.root) }
            }
        }
    }

    protected open fun contentForPackage(p: DPackage) = contentBuilder.contentFor(p) {
        group(kind = ContentKind.Cover) {
            cover("Package ${p.name}")
            if (contentForDescription(p).isNotEmpty()) {
                sourceSetDependentHint(
                    p.dri,
                    p.sourceSets.toSet(),
                    kind = ContentKind.SourceSetDependentHint,
                    styles = setOf(TextStyle.UnderCoverText)
                ) {
                    +contentForDescription(p)
                }
            }
        }
        group(styles = setOf(ContentStyle.TabbedContent)) {
            +contentForComments(p)
            +contentForScope(p, p.dri, p.sourceSets)
        }
    }

    protected open fun contentForScope(
        s: WithScope,
        dri: DRI,
        sourceSets: Set<DokkaSourceSet>
    ) = contentBuilder.contentFor(s as Documentable) {
        val types = listOf(
            s.classlikes,
            (s as? DPackage)?.typealiases ?: emptyList()
        ).flatten()
        divergentBlock("Types", types, ContentKind.Classlikes, extra = mainExtra + SimpleAttr.header("Types"))
        if (separateInheritedMembers) {
            val (inheritedFunctions, memberFunctions) = s.functions.splitInherited()
            val (inheritedProperties, memberProperties) = s.properties.splitInherited()
            propertiesBlock("Properties", memberProperties, sourceSets)
            propertiesBlock("Inherited properties", inheritedProperties, sourceSets)
            functionsBlock("Functions", memberFunctions)
            functionsBlock("Inherited functions", inheritedFunctions)
        } else {
            functionsBlock("Functions", s.functions)
            propertiesBlock("Properties", s.properties, sourceSets)
        }
        s.safeAs<WithExtraProperties<Documentable>>()?.let { it.extra[InheritorsInfo] }?.let { inheritors ->
            val map = inheritors.value.filter { it.value.isNotEmpty() }
            if (map.values.any()) {
                header(2, "Inheritors") { }
                +ContentTable(
                    header = listOf(contentBuilder.contentFor(mainDRI, mainSourcesetData) {
                        text("Name")
                    }),
                    children = map.entries.flatMap { entry -> entry.value.map { Pair(entry.key, it) } }
                        .groupBy({ it.second }, { it.first }).map { (classlike, platforms) ->
                            val label = classlike.classNames?.substringBeforeLast(".") ?: classlike.toString()
                                .also { logger.warn("No class name found for DRI $classlike") }
                            buildGroup(
                                setOf(classlike),
                                platforms.toSet(),
                                ContentKind.Inheritors,
                                extra = mainExtra + SymbolAnchorHint(label, ContentKind.Inheritors)
                            ) {
                                link(label, classlike)
                            }
                        },
                    dci = DCI(setOf(dri), ContentKind.Inheritors),
                    sourceSets = sourceSets.toDisplaySourceSets(),
                    style = emptySet(),
                    extra = mainExtra + SimpleAttr.header("Inheritors")
                )
            }
        }
    }

    private fun Iterable<DFunction>.sorted() =
        sortedWith(compareBy({ it.name }, { it.parameters.size }, { it.dri.toString() }))

    protected open fun contentForEnumEntry(e: DEnumEntry) = contentBuilder.contentFor(e) {
        group(kind = ContentKind.Cover) {
            cover(e.name)
            sourceSetDependentHint(e.dri, e.sourceSets.toSet()) {
                +buildSignature(e)
                +contentForDescription(e)
            }
        }
        group(styles = setOf(ContentStyle.TabbedContent)) {
            +contentForComments(e)
            +contentForScope(e, e.dri, e.sourceSets)
        }
    }

    protected open fun contentForClasslike(c: DClasslike) = contentBuilder.contentFor(c) {
        @Suppress("UNCHECKED_CAST")
        val extensions = (c as WithExtraProperties<DClasslike>)
            .extra[CallableExtensions]?.extensions
            ?.filterIsInstance<Documentable>().orEmpty()
        // Extensions are added to sourceSets since they can be placed outside the sourceSets from classlike
        // Example would be an Interface in common and extension function in jvm
        group(kind = ContentKind.Cover, sourceSets = mainSourcesetData + extensions.sourceSets) {
            cover(c.name.orEmpty())
            sourceSetDependentHint(c.dri, c.sourceSets) {
                +buildSignature(c)
                +contentForDescription(c)
            }
        }

        group(styles = setOf(ContentStyle.TabbedContent), sourceSets = mainSourcesetData + extensions.sourceSets) {
            +contentForComments(c)
            if (c is WithConstructors) {
                block(
                    "Constructors",
                    2,
                    ContentKind.Constructors,
                    c.constructors.filter { it.extra[PrimaryConstructorExtra] == null || it.documentation.isNotEmpty() },
                    c.sourceSets,
                    needsAnchors = true,
                    extra = PropertyContainer.empty<ContentNode>() + SimpleAttr.header("Constructors")
                ) {
                    link(it.name, it.dri, kind = ContentKind.Main)
                    sourceSetDependentHint(
                        it.dri,
                        it.sourceSets.toSet(),
                        kind = ContentKind.SourceSetDependentHint,
                        styles = emptySet(),
                        extra = PropertyContainer.empty<ContentNode>()
                    ) {
                        +buildSignature(it)
                        contentForBrief(it)
                    }
                }
            }
            if (c is DEnum) {
                block(
                    "Entries",
                    2,
                    ContentKind.Classlikes,
                    c.entries,
                    c.sourceSets.toSet(),
                    needsSorting = false,
                    needsAnchors = true,
                    extra = mainExtra + SimpleAttr.header("Entries"),
                    styles = emptySet()
                ) {
                    link(it.name, it.dri)
                    sourceSetDependentHint(
                        it.dri,
                        it.sourceSets.toSet(),
                        kind = ContentKind.SourceSetDependentHint,
                        extra = PropertyContainer.empty<ContentNode>()
                    ) {
                        +buildSignature(it)
                        contentForBrief(it)
                    }
                }
            }
            +contentForScope(c, c.dri, c.sourceSets)

            divergentBlock(
                "Extensions",
                extensions,
                ContentKind.Extensions,
                extra = mainExtra + SimpleAttr.header("Extensions")
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : TagWrapper> GroupedTags.withTypeUnnamed(): SourceSetDependent<T> =
        (this[T::class] as List<Pair<DokkaSourceSet, T>>?)?.toMap().orEmpty()

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : NamedTagWrapper> GroupedTags.withTypeNamed(): Map<String, SourceSetDependent<T>> =
        (this[T::class] as List<Pair<DokkaSourceSet, T>>?)
            ?.groupByTo(linkedMapOf()) { it.second.name }
            ?.mapValues { (_, v) -> v.toMap() }
            .orEmpty()

    private inline fun <reified T : TagWrapper> GroupedTags.isNotEmptyForTag(): Boolean =
        this[T::class]?.isNotEmpty() ?: false

    protected open fun contentForDescription(
        d: Documentable
    ): List<ContentNode> {
        val tags: GroupedTags = d.groupedTags
        val platforms = d.sourceSets.toSet()

        return contentBuilder.contentFor(d, styles = setOf(TextStyle.Block)) {
            val descriptions = d.descriptions
            if (descriptions.any { it.value.root.children.isNotEmpty() }) {
                platforms.forEach { platform ->
                    descriptions[platform]?.also {
                        group(sourceSets = setOf(platform), styles = emptySet()) {
                            comment(it.root)
                        }
                    }
                }
            }

            val unnamedTags = tags.filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in specialTags }
                .values.flatten().groupBy { it.first }.mapValues { it.value.map { it.second } }
            if (unnamedTags.isNotEmpty()) {
                platforms.forEach { platform ->
                    unnamedTags[platform]?.let { tags ->
                        if (tags.isNotEmpty()) {
                            tags.groupBy { it::class }.forEach { (_, sameCategoryTags) ->
                                group(sourceSets = setOf(platform), styles = emptySet()) {
                                    header(4, sameCategoryTags.first().toHeaderString())
                                    sameCategoryTags.forEach { comment(it.root) }
                                }
                            }
                        }
                    }
                }
            }

            contentForSinceKotlin(d)
        }.children
    }

    private fun Documentable.getPossibleFallbackSourcesets(sourceSet: DokkaSourceSet) =
        this.sourceSets.filter { it.sourceSetID in sourceSet.dependentSourceSets }

    private fun <V> Map<DokkaSourceSet, V>.fallback(sourceSets: List<DokkaSourceSet>): V? =
        sourceSets.firstOrNull { it in this.keys }.let { this[it] }

    protected open fun contentForComments(
        d: Documentable
    ): List<ContentNode> {
        val tags = d.groupedTags
        val platforms = d.sourceSets

        fun DocumentableContentBuilder.contentForParams() {
            if (tags.isNotEmptyForTag<Param>()) {
                header(2, "Parameters", kind = ContentKind.Parameters)
                group(
                    extra = mainExtra + SimpleAttr.header("Parameters"),
                    styles = setOf(ContentStyle.WithExtraAttributes)
                ) {
                    sourceSetDependentHint(sourceSets = platforms.toSet(), kind = ContentKind.SourceSetDependentHint) {
                        val receiver = tags.withTypeUnnamed<Receiver>()
                        val params = tags.withTypeNamed<Param>()
                        table(kind = ContentKind.Parameters) {
                            platforms.forEach { platform ->
                                val possibleFallbacks = d.getPossibleFallbackSourcesets(platform)
                                (receiver[platform] ?: receiver.fallback(possibleFallbacks))?.let {
                                    row(sourceSets = setOf(platform), kind = ContentKind.Parameters) {
                                        text("<receiver>", styles = mainStyles + ContentStyle.RowTitle)
                                        comment(it.root)
                                    }
                                }

                                params.mapNotNull { (_, param) ->
                                    (param[platform] ?: param.fallback(possibleFallbacks))?.let {
                                        row(sourceSets = setOf(platform), kind = ContentKind.Parameters) {
                                            text(
                                                it.name,
                                                kind = ContentKind.Parameters,
                                                styles = mainStyles + ContentStyle.RowTitle
                                            )
                                            comment(it.root)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForSeeAlso() {
            if (tags.isNotEmptyForTag<See>()) {
                header(2, "See also", kind = ContentKind.Comment)
                group(
                    extra = mainExtra + SimpleAttr.header("See also"),
                    styles = setOf(ContentStyle.WithExtraAttributes)
                ) {
                    sourceSetDependentHint(sourceSets = platforms.toSet(), kind = ContentKind.SourceSetDependentHint) {
                        val seeAlsoTags = tags.withTypeNamed<See>()
                        table(kind = ContentKind.Sample) {
                            platforms.forEach { platform ->
                                val possibleFallbacks = d.getPossibleFallbackSourcesets(platform)
                                seeAlsoTags.forEach { (_, see) ->
                                    (see[platform] ?: see.fallback(possibleFallbacks))?.let {
                                        row(
                                            sourceSets = setOf(platform),
                                            kind = ContentKind.Comment,
                                            styles = this@sourceSetDependentHint.mainStyles + ContentStyle.RowTitle,
                                        ) {
                                            if (it.address != null) link(
                                                it.name,
                                                it.address!!,
                                                kind = ContentKind.Comment
                                            )
                                            else text(it.name, kind = ContentKind.Comment)
                                            comment(it.root)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForThrows() {
            val throws = tags.withTypeNamed<Throws>()
            if (throws.isNotEmpty()) {
                header(4, "Throws")
                sourceSetDependentHint(sourceSets = platforms.toSet(), kind = ContentKind.SourceSetDependentHint) {
                    platforms.forEach { sourceset ->
                        table(kind = ContentKind.Main, sourceSets = setOf(sourceset)) {
                            throws.entries.forEach { entry ->
                                entry.value[sourceset]?.let { throws ->
                                    row(sourceSets = setOf(sourceset)) {
                                        group(styles = mainStyles + ContentStyle.RowTitle) {
                                            throws.exceptionAddress?.let {
                                                link(text = entry.key, address = it)
                                            } ?: text(entry.key)
                                        }
                                        comment(throws.root)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForSamples() {
            val samples = tags.withTypeNamed<Sample>()
            if (samples.isNotEmpty()) {
                header(2, "Samples", kind = ContentKind.Sample)
                group(
                    extra = mainExtra + SimpleAttr.header("Samples"),
                    styles = emptySet()
                ) {
                    sourceSetDependentHint(sourceSets = platforms.toSet(), kind = ContentKind.SourceSetDependentHint) {
                        platforms.map { platformData ->
                            val content = samples.filter { it.value.isEmpty() || platformData in it.value }
                            group(
                                sourceSets = setOf(platformData),
                                kind = ContentKind.Sample,
                                styles = setOf(TextStyle.Monospace, ContentStyle.RunnableSample)
                            ) {
                                content.forEach {
                                    text(it.key)
                                }
                            }
                        }
                    }
                }
            }
        }

        return contentBuilder.contentFor(d) {
            if (tags.isNotEmpty()) {
                contentForSamples()
                contentForSeeAlso()
                contentForParams()
                contentForThrows()
            }
        }.children
    }

    protected open fun DocumentableContentBuilder.contentForBrief(documentable: Documentable) {
        documentable.sourceSets.forEach { sourceSet ->
            documentable.documentation[sourceSet]?.children?.firstOrNull()?.root?.let {
                group(sourceSets = setOf(sourceSet), kind = ContentKind.BriefComment) {
                    if (documentable.hasSeparatePage) firstSentenceComment(it)
                    else comment(it)
                }
            }
        }
    }

    protected open fun DocumentableContentBuilder.contentForSinceKotlin(documentable: Documentable) {
        documentable.documentation.mapValues {
            it.value.children.find { it is CustomTagWrapper && it.name == "Since Kotlin" } as CustomTagWrapper?
        }.run {
            documentable.sourceSets.forEach { sourceSet ->
                this[sourceSet]?.also { tag ->
                    group(sourceSets = setOf(sourceSet), kind = ContentKind.Comment, styles = setOf(TextStyle.Block)) {
                        header(4, tag.name)
                        comment(tag.root)
                    }
                }
            }
        }
    }

    protected open fun contentForFunction(f: DFunction) = contentForMember(f)

    protected open fun contentForProperty(p: DProperty) = contentForMember(p)

    protected open fun contentForMember(d: Documentable) = contentBuilder.contentFor(d) {
        group(kind = ContentKind.Cover) {
            cover(d.name.orEmpty())
        }
        divergentGroup(ContentDivergentGroup.GroupID("member")) {
            instance(setOf(d.dri), d.sourceSets.toSet()) {
                divergent {
                    +buildSignature(d)
                }
                after {
                    +contentForDescription(d)
                    +contentForComments(d)
                }
            }
        }
    }

    private fun DocumentableContentBuilder.functionsBlock(name: String, list: Collection<DFunction>) = divergentBlock(
        name,
        list.sorted(),
        ContentKind.Functions,
        extra = mainExtra + SimpleAttr.header(name)
    )

    private fun DocumentableContentBuilder.propertiesBlock(
        name: String,
        list: Collection<DProperty>,
        sourceSets: Set<DokkaSourceSet>
    ) {
        block(
            name,
            2,
            ContentKind.Properties,
            list,
            sourceSets,
            needsAnchors = true,
            extra = mainExtra + SimpleAttr.header(name),
            headers = listOf(
                headers("Name", "Summary")
            )
        ) {
            link(it.name, it.dri, kind = ContentKind.Main)
            sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependentHint) {
                +buildSignature(it)
                contentForBrief(it)
            }
        }
    }

    protected open fun DocumentableContentBuilder.divergentBlock(
        name: String,
        collection: Collection<Documentable>,
        kind: ContentKind,
        extra: PropertyContainer<ContentNode> = mainExtra
    ) {
        if (collection.any()) {
            header(2, name, kind = kind)
            table(kind, extra = extra, styles = emptySet()) {
                header {
                    group { text("Name") }
                    group { text("Summary") }
                }
                collection
                    .groupBy { it.name } // This groupBy should probably use LocationProvider
                    // This hacks displaying actual typealias signatures along classlike ones
                    .mapValues { if (it.value.any { it is DClasslike }) it.value.filter { it !is DTypeAlias } else it.value }
                    .toSortedMap(compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it })
                    .forEach { (elementName, elements) -> // This groupBy should probably use LocationProvider
                        row(
                            dri = elements.map { it.dri }.toSet(),
                            sourceSets = elements.flatMap { it.sourceSets }.toSet(),
                            kind = kind,
                            styles = emptySet(),
                            extra = elementName?.let { name -> extra + SymbolAnchorHint(name, kind) } ?: extra
                        ) {
                            link(elementName.orEmpty(), elements.first().dri, kind = kind)
                            divergentGroup(
                                ContentDivergentGroup.GroupID(name),
                                elements.map { it.dri }.toSet(),
                                kind = kind,
                                extra = extra
                            ) {
                                elements.map {
                                    instance(
                                        setOf(it.dri),
                                        it.sourceSets.toSet(),
                                        extra = PropertyContainer.withAll(SymbolAnchorHint(it.name ?: "", kind))
                                    ) {
                                        divergent(extra = PropertyContainer.empty()) {
                                            group {
                                                +buildSignature(it)
                                            }
                                        }
                                        after {
                                            contentForBrief(it)
                                            contentForSinceKotlin(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }


    protected open fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()

    private val List<Documentable>.sourceSets: Set<DokkaSourceSet>
        get() = flatMap { it.sourceSets }.toSet()

    private val Documentable.groupedTags: GroupedTags
        get() = documentation.flatMap { (pd, doc) ->
            doc.children.asSequence().map { pd to it }.toList()
        }.groupBy { it.second::class }

    private val Documentable.descriptions: SourceSetDependent<Description>
        get() = groupedTags.withTypeUnnamed<Description>()

    private val Documentable.hasSeparatePage: Boolean
        get() = this !is DTypeAlias

    private fun <T : Documentable> T.nameAfterClash(): String =
        ((this as? WithExtraProperties<out Documentable>)?.extra?.get(DriClashAwareName)?.value ?: name).orEmpty()
}
