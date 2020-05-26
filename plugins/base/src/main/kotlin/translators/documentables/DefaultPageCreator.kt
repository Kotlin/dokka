package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.signatures.SignatureProvider
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

private typealias GroupedTags = Map<KClass<out TagWrapper>, List<Pair<SourceSetData?, TagWrapper>>>

private val specialTags: Set<KClass<out TagWrapper>> =
    setOf(Property::class, Description::class, Constructor::class, Receiver::class, Param::class, See::class)


open class DefaultPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    protected open val contentBuilder = PageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    open fun pageForModule(m: DModule) =
        ModulePageNode(m.name.ifEmpty { "<root>" }, contentForModule(m), m, m.packages.map(::pageForPackage))

    open fun pageForPackage(p: DPackage): PackagePageNode = PackagePageNode(
        p.name, contentForPackage(p), setOf(p.dri), p,
        p.classlikes.map(::pageForClasslike) +
                p.functions.map(::pageForFunction)
    )

    open fun pageForEnumEntry(e: DEnumEntry): ClasslikePageNode =
        ClasslikePageNode(
            e.name, contentForEnumEntry(e), setOf(e.dri), e,
            e.classlikes.map(::pageForClasslike) +
                    e.filteredFunctions.map(::pageForFunction)
        )

    open fun pageForClasslike(c: DClasslike): ClasslikePageNode {
        val constructors = if (c is WithConstructors) c.constructors else emptyList()

        return ClasslikePageNode(
            c.name.orEmpty(), contentForClasslike(c), setOf(c.dri), c,
            constructors.map(::pageForFunction) +
                    c.classlikes.map(::pageForClasslike) +
                    c.filteredFunctions.map(::pageForFunction) +
                    if (c is DEnum) c.entries.map(::pageForEnumEntry) else emptyList()
        )
    }

    open fun pageForFunction(f: DFunction) = MemberPageNode(f.name, contentForFunction(f), setOf(f.dri), f)

    open fun pageForTypeAlias(t: DTypeAlias) = MemberPageNode(t.name, contentForTypeAlias(t), setOf(t.dri), t)

    private val WithScope.filteredFunctions
        get() = functions.filter { it.extra[InheritedFunction]?.isInherited != true }

    protected open fun contentForModule(m: DModule) = contentBuilder.contentFor(m) {
        group(kind = ContentKind.Cover) {
            header(1, m.name)
            sourceSetDependentHint(m.dri, m.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint){
                +contentForDescription(m)
            }
        }
        +contentForComments(m)
        block("Packages", 2, ContentKind.Packages, m.packages, m.sourceSets.toSet()) {
            link(it.name, it.dri)
        }
//        text("Index\n") TODO
//        text("Link to allpage here")
    }

    protected open fun contentForPackage(p: DPackage) = contentBuilder.contentFor(p) {
        group(kind = ContentKind.Cover) {
            header(1, "Package ${p.name}")
            sourceSetDependentHint(p.dri, p.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint){
                +contentForDescription(p)
            }
        }
        group(styles = setOf(ContentStyle.TabbedContent)){
            +contentForComments(p)
            +contentForScope(p, p.dri, p.sourceSets)
            block("Type aliases", 2, ContentKind.TypeAliases, p.typealiases, p.sourceSets.toSet(), extra = mainExtra + SimpleAttr.header("Type aliases")) {
                link(it.name, it.dri, kind = ContentKind.Main)
                sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint, styles = emptySet()) {
                    +buildSignature(it)
                    contentForBrief(it)
                }
            }
        }
    }

    protected open fun contentForScope(
        s: WithScope,
        dri: DRI,
        sourceSets: List<SourceSetData>
    ) = contentBuilder.contentFor(s as Documentable) {
        divergentBlock("Types", s.classlikes, ContentKind.Classlikes, extra = mainExtra + SimpleAttr.header("Types"))
        divergentBlock("Functions", s.functions, ContentKind.Functions, extra = mainExtra + SimpleAttr.header( "Functions"))
        block("Properties", 2, ContentKind.Properties, s.properties, sourceSets.toSet(), extra = mainExtra + SimpleAttr.header( "Properties")) {
            link(it.name, it.dri, kind = ContentKind.Main)
            sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint) {
                +buildSignature(it)

                contentForBrief(it)
            }
        }
        s.safeAs<WithExtraProperties<Documentable>>()?.let { it.extra[InheritorsInfo] }?.let { inheritors ->
            val map = inheritors.value.filter { it.value.isNotEmpty() }
            if (map.values.any()) {
                header(2, "Inheritors") { }
                +ContentTable(
                    emptyList(),
                    map.entries.flatMap { entry -> entry.value.map { Pair(entry.key, it) } }
                        .groupBy({ it.second }, { it.first }).map { (classlike, platforms) ->
                            buildGroup(setOf(dri), platforms.toSet(), ContentKind.Inheritors) {
                                link(
                                    classlike.classNames?.substringBeforeLast(".") ?: classlike.toString()
                                        .also { logger.warn("No class name found for DRI $classlike") }, classlike
                                )
                            }
                        },
                    DCI(setOf(dri), ContentKind.Inheritors),
                    sourceSets.toSet(),
                    style = emptySet(),
                    extra = mainExtra + SimpleAttr.header( "Inheritors")
                )
            }
        }
    }

    protected open fun contentForEnumEntry(e: DEnumEntry) = contentBuilder.contentFor(e) {
        group(kind = ContentKind.Cover) {
            header(1, e.name)
            +buildSignature(e)
            +contentForDescription(e)
        }
        group(styles = setOf(ContentStyle.TabbedContent)){
            +contentForComments(e)
            +contentForScope(e, e.dri, e.sourceSets)
        }
    }

    protected open fun contentForClasslike(c: DClasslike) = contentBuilder.contentFor(c) {
        group(kind = ContentKind.Cover) {
            header(1, c.name.orEmpty())
                sourceSetDependentHint(c.dri, c.sourceSets.toSet()) {
                +buildSignature(c)
                +contentForDescription(c)
            }
        }

        group(styles = setOf(ContentStyle.TabbedContent)) {
            +contentForComments(c)
            if (c is WithConstructors) {
                block(
                    "Constructors",
                    2,
                    ContentKind.Constructors,
                    c.constructors.filter { it.extra[PrimaryConstructorExtra] == null },
                    c.sourceSets.toSet(),
                    extra = PropertyContainer.empty<ContentNode>() + SimpleAttr.header("Constructors")
                ) {
                    link(it.name, it.dri, kind = ContentKind.Main)
                    sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint, styles = emptySet()) {
                        +buildSignature(it)
                        contentForBrief(it)
                    }
                }
            }
            if (c is DEnum) {
                block("Entries", 2, ContentKind.Classlikes, c.entries, c.sourceSets.toSet(), extra = mainExtra + SimpleAttr.header("Entries"), styles = emptySet()) {
                    link(it.name, it.dri)
                    sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint) {
                        +buildSignature(it)
                        contentForBrief(it)
                    }
                }
            }
            +contentForScope(c, c.dri, c.sourceSets)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : TagWrapper> GroupedTags.withTypeUnnamed(): SourceSetDependent<T> =
        (this[T::class] as List<Pair<SourceSetData, T>>?)?.toMap().orEmpty()

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : NamedTagWrapper> GroupedTags.withTypeNamed(): Map<String, SourceSetDependent<T>> =
        (this[T::class] as List<Pair<SourceSetData, T>>?)
            ?.groupBy { it.second.name }
            ?.mapValues { (_, v) -> v.toMap() }
            .orEmpty()

    private inline fun <reified T : TagWrapper> GroupedTags.isNotEmptyForTag(): Boolean =
        this[T::class]?.isNotEmpty() ?: false

    protected open fun contentForDescription(
        d: Documentable
    ): List<ContentNode> {
        val tags: GroupedTags = d.documentation.flatMap { (pd, doc) ->
            doc.children.asSequence().map { pd to it }.toList()
        }.groupBy { it.second::class }

        val platforms = d.sourceSets.toSet()

        return contentBuilder.contentFor(d) {
            val description = tags.withTypeUnnamed<Description>()
            if (description.any { it.value.root.children.isNotEmpty() }) {
                platforms.forEach { platform ->
                    description[platform]?.also {
                        group(sourceSets = setOf(platform)) {
                            comment(it.root)
                        }
                    }
                }
            }

            val unnamedTags: List<SourceSetDependent<TagWrapper>> =
                tags.filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in specialTags }
                    .map { (_, v) -> v.mapNotNull { (k,v) -> k?.let { it to v } }.toMap() }
            if(unnamedTags.isNotEmpty()){
                platforms.forEach { platform ->
                    unnamedTags.forEach { pdTag ->
                        pdTag[platform]?.also { tag ->
                            group(sourceSets = setOf(platform)) {
                                header(4, tag.toHeaderString())
                                comment(tag.root)
                            }
                        }
                    }
                }
            }
        }.children
    }

    protected open fun contentForComments(
        d: Documentable
    ): List<ContentNode> {
        val tags: GroupedTags = d.documentation.flatMap { (pd, doc) ->
            doc.children.asSequence().map { pd to it }.toList()
        }.groupBy { it.second::class }

        val platforms = d.sourceSets

        fun DocumentableContentBuilder.contentForParams() {
            if (tags.isNotEmptyForTag<Param>()) {
                header(2, "Parameters")
                group(extra = mainExtra + SimpleAttr.header("Parameters"), styles = setOf(ContentStyle.WithExtraAttributes)){
                    sourceSetDependentHint(sourceSets = platforms.toSet(), kind = ContentKind.SourceSetDependantHint) {
                        val receiver = tags.withTypeUnnamed<Receiver>()
                        val params = tags.withTypeNamed<Param>()
                        table(kind = ContentKind.Parameters) {
                            platforms.flatMap { platform ->
                                val receiverRow = receiver[platform]?.let {
                                    buildGroup(sourceSets = setOf(platform), kind = ContentKind.Parameters) {
                                        text("<receiver>", styles = mainStyles + ContentStyle.RowTitle)
                                        comment(it.root)
                                    }
                                }

                                val paramRows = params.mapNotNull { (_, param) ->
                                    param[platform]?.let {
                                        buildGroup(sourceSets = setOf(platform), kind = ContentKind.Parameters) {
                                            text(it.name, kind = ContentKind.Parameters, styles = mainStyles + ContentStyle.RowTitle)
                                            comment(it.root)
                                        }
                                    }
                                }

                                listOfNotNull(receiverRow) + paramRows
                            }
                        }
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForSeeAlso() {
            if (tags.isNotEmptyForTag<See>()) {
                header(2, "See also")
                group(extra = mainExtra + SimpleAttr.header("See also"), styles = setOf(ContentStyle.WithExtraAttributes)){
                    sourceSetDependentHint(sourceSets = platforms.toSet(), kind = ContentKind.SourceSetDependantHint) {
                        val seeAlsoTags = tags.withTypeNamed<See>()
                        table(kind = ContentKind.Sample) {
                            platforms.flatMap { platform ->
                                seeAlsoTags.mapNotNull { (_, see) ->
                                    see[platform]?.let {
                                        buildGroup(sourceSets = setOf(platform), kind = ContentKind.Comment, styles = mainStyles + ContentStyle.RowTitle) {
                                            if (it.address != null) link(it.name, it.address!!, kind = ContentKind.Comment)
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

        fun DocumentableContentBuilder.contentForSamples() {
            val samples = tags.withTypeNamed<Sample>()
            if (samples.isNotEmpty()) {
                header(2, "Samples")
                group(extra = mainExtra + SimpleAttr.header("Samples"), styles = setOf(ContentStyle.WithExtraAttributes)){
                    sourceSetDependentHint(sourceSets = platforms.toSet(), kind = ContentKind.SourceSetDependantHint) {
                        table(kind = ContentKind.Sample) {
                            platforms.map { platformData ->
                                val content = samples.filter { it.value.isEmpty() || platformData in it.value }
                                buildGroup(sourceSets = setOf(platformData), styles = setOf(ContentStyle.RowTitle)) {
                                    content.forEach {
                                        comment(Text(it.key))
                                    }
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
            }
        }.children
    }

    protected open fun DocumentableContentBuilder.contentForBrief(content: Documentable) {
        content.sourceSets.forEach { sourceSet ->
            content.documentation[sourceSet]?.children?.firstOrNull()?.root?.let {
                group(sourceSets = setOf(sourceSet), kind = ContentKind.BriefComment) {
                    comment(it)
                }
            }
        }
    }

    protected open fun contentForFunction(f: DFunction) = contentForMember(f)
    protected open fun contentForTypeAlias(t: DTypeAlias) = contentForMember(t)
    protected open fun contentForMember(d: Documentable) = contentBuilder.contentFor(d) {
        group(kind = ContentKind.Cover) {
            header(1, d.name.orEmpty())
        }
        divergentGroup(ContentDivergentGroup.GroupID("member")) {
            instance(setOf(d.dri), d.sourceSets.toSet()) {
                divergent(kind = ContentKind.Symbol) {
                    +buildSignature(d)
                }
                after {
                    +contentForDescription(d)
                    +contentForComments(d)
                }
            }
        }
    }

    protected open fun DocumentableContentBuilder.divergentBlock(
        name: String,
        collection: Collection<Documentable>,
        kind: ContentKind,
        extra: PropertyContainer<ContentNode> = mainExtra,
    ) {
        if (collection.any()) {
            header(2, name)
            table(kind, extra = extra) {
                collection.groupBy { it.name }.map { (elementName, elements) -> // This groupBy should probably use LocationProvider
                    buildGroup(elements.map { it.dri }.toSet(), elements.flatMap { it.sourceSets }.toSet(), kind = kind) {
                        link(elementName.orEmpty(), elements.first().dri, kind = kind)
                        divergentGroup(
                            ContentDivergentGroup.GroupID(name),
                            elements.map { it.dri }.toSet(),
                            kind = kind
                        ) {
                            elements.map {
                                instance(setOf(it.dri), it.sourceSets.toSet()) {
                                    divergent {
                                        group(kind = ContentKind.Symbol) {
                                            +buildSignature(it)
                                        }
                                    }
                                    after {
                                        contentForBrief(it)
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
}
