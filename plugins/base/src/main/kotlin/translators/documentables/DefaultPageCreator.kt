package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
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
                p.functions.map(::pageForFunction) +
                p.typealiases.map(::pageForTypeAlias)
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
            header(1) { text(m.name) }
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
            header(1) { text("Package ${p.name}") }
        }
        +contentForComments(p)
        +contentForScope(p, p.dri, p.sourceSets)
        block("Type aliases", 2, ContentKind.TypeAliases, p.typealiases, p.sourceSets.toSet()) {
            link(it.name, it.dri, kind = ContentKind.Main)
            sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint) {
                +buildSignature(it)
                contentForBrief(it)
            }
        }
    }

    protected open fun contentForScope(
        s: WithScope,
        dri: DRI,
        sourceSets: List<SourceSetData>
    ) = contentBuilder.contentFor(s as Documentable) {
        divergentBlock("Types", s.classlikes, ContentKind.Classlikes)
        divergentBlock("Functions", s.functions, ContentKind.Functions)
        block("Properties", 2, ContentKind.Properties, s.properties, sourceSets.toSet()) {
            link(it.name, it.dri, kind = ContentKind.Main)
            sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint) {
                +buildSignature(it)

                contentForBrief(it)
            }
        }
        s.safeAs<WithExtraProperties<Documentable>>()?.let { it.extra[InheritorsInfo] }?.let { inheritors ->
            val map = inheritors.value.filter { it.value.isNotEmpty() }
            if (map.values.any()) {
                header(2) { text("Inheritors") }
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
                    style = emptySet()
                )
            }
        }
    }

    protected open fun contentForEnumEntry(e: DEnumEntry) = contentBuilder.contentFor(e) {
        group(kind = ContentKind.Cover) {
            header(1) { text(e.name) }
            +buildSignature(e)
        }
        +contentForComments(e)
        +contentForScope(e, e.dri, e.sourceSets)
    }

    protected open fun contentForClasslike(c: DClasslike) = contentBuilder.contentFor(c) {
        group(kind = ContentKind.Cover) {
            header(1) { text(c.name.orEmpty()) }
            sourceSetDependentHint(c.dri, c.sourceSets.toSet()) {
                +buildSignature(c)
            }
        }
        +contentForComments(c)

        if (c is WithConstructors) {
            block(
                "Constructors",
                2,
                ContentKind.Constructors,
                c.constructors.filter { it.extra[PrimaryConstructorExtra] == null },
                c.sourceSets.toSet()
            ) {
                link(it.name, it.dri, kind = ContentKind.Main)
                sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint) {
                    +buildSignature(it)
                    contentForBrief(it)
                }
            }
        }
        if (c is DEnum) {
            block("Entries", 2, ContentKind.Classlikes, c.entries, c.sourceSets.toSet()) {
                link(it.name, it.dri, kind = ContentKind.Main)
                sourceSetDependentHint(it.dri, it.sourceSets.toSet(), kind = ContentKind.SourceSetDependantHint) {
                    +buildSignature(it)
                    contentForBrief(it)
                }
            }
        }

        +contentForScope(c, c.dri, c.sourceSets)
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

    protected open fun contentForComments(
        d: Documentable
    ): List<ContentNode> {
        val tags: GroupedTags = d.documentation.flatMap { (pd, doc) ->
            doc.children.asSequence().map { pd to it }.toList()
        }.groupBy { it.second::class }

        val platforms = d.sourceSets

        fun DocumentableContentBuilder.contentForDescription() {
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
        }

        fun DocumentableContentBuilder.contentForParams() {
            if (tags.isNotEmptyForTag<Param>()) {
                val receiver = tags.withTypeUnnamed<Receiver>()
                val params = tags.withTypeNamed<Param>()
                platforms.forEach {
                    header(4, kind = ContentKind.Parameters, platformData = setOf(it)) { text("Parameters") }
                }
                table(kind = ContentKind.Parameters) {
                    platforms.flatMap { platform ->
                        val receiverRow = receiver[platform]?.let {
                            buildGroup(sourceSets = setOf(platform), kind = ContentKind.Parameters, styles = mainStyles + ContentStyle.KeyValue) {
                                text("<receiver>")
                                comment(it.root)
                            }
                        }

                        val paramRows = params.mapNotNull { (_, param) ->
                            param[platform]?.let {
                                buildGroup(sourceSets = setOf(platform), kind = ContentKind.Parameters, styles = mainStyles + ContentStyle.KeyValue) {
                                    text(it.name, kind = ContentKind.Parameters)
                                    comment(it.root)
                                }
                            }
                        }

                        listOfNotNull(receiverRow) + paramRows
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForSeeAlso() {
            if (tags.isNotEmptyForTag<See>()) {
                val seeAlsoTags = tags.withTypeNamed<See>()
                platforms.forEach {
                    header(4, kind = ContentKind.Comment, platformData = setOf(it)) { text("See also") }
                }
                table(kind = ContentKind.Sample) {
                    platforms.flatMap { platform ->
                        seeAlsoTags.mapNotNull { (_, see) ->
                            see[platform]?.let {
                                buildGroup(sourceSets = setOf(platform), kind = ContentKind.Comment, styles = mainStyles + ContentStyle.KeyValue) {
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

        fun DocumentableContentBuilder.contentForSamples() {
            val samples = tags.withTypeNamed<Sample>()
            if (samples.isNotEmpty()) {
                platforms.forEach { platformData ->
                    val content = samples.filter { it.value.isEmpty() || platformData in it.value }
                    if (content.isNotEmpty()) {
                        group(sourceSets = setOf(platformData)) {
                            header(4, kind = ContentKind.Comment) { text("Samples") }
                            content.forEach {
                                comment(Text(it.key))
                            }
                        }
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForUnnamedTags() {
            val unnamedTags: List<SourceSetDependent<TagWrapper>> =
                tags.filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in specialTags }
                    .map { (_, v) -> v.mapNotNull { (k,v) -> k?.let { it to v } }.toMap() }
            platforms.forEach { platform ->
                unnamedTags.forEach { pdTag ->
                    pdTag[platform]?.also { tag ->
                        group(sourceSets = setOf(platform)) {
                            header(4) { text(tag.toHeaderString()) }
                            comment(tag.root)
                        }
                    }
                }
            }
        }

        return contentBuilder.contentFor(d) {
            if (tags.isNotEmpty()) {
                header(3) { text("Description") }
                sourceSetDependentHint(sourceSets = platforms.toSet()) {
                    contentForDescription()
                    contentForSamples()
                    contentForParams()
                    contentForUnnamedTags()
                    contentForSeeAlso()
                }
            }
        }.children
    }

    protected open fun DocumentableContentBuilder.contentForBrief(content: Documentable) {
        content.sourceSets.forEach { platform ->
            val root = content.documentation[platform]?.children?.firstOrNull()?.root

            root?.let {
                group(sourceSets = setOf(platform), kind = ContentKind.BriefComment) {
                    text(it.docTagSummary(), kind = ContentKind.Comment)
                }
            }
        }
    }

    protected open fun contentForFunction(f: DFunction) = contentForMember(f)
    protected open fun contentForTypeAlias(t: DTypeAlias) = contentForMember(t)
    protected open fun contentForMember(d: Documentable) = contentBuilder.contentFor(d) {
        header(1) { text(d.name.orEmpty()) }
        divergentGroup(ContentDivergentGroup.GroupID("member")) {
            instance(setOf(d.dri), d.sourceSets.toSet()) {
                divergent(kind = ContentKind.Symbol) {
                    +buildSignature(d)
                }
                after {
                    +contentForComments(d)
                }
            }
        }
    }

    protected open fun DocumentableContentBuilder.divergentBlock(
        name: String,
        collection: Collection<Documentable>,
        kind: ContentKind
    ) {
        if (collection.any()) {
            header(2) { text(name) }
            table(kind) {
                collection.groupBy { it.name }.map { (elementName, elements) -> // This groupBy should probably use LocationProvider
                    buildGroup(elements.map { it.dri }.toSet(), elements.flatMap { it.sourceSets }.toSet()) {
                        link(elementName.orEmpty(), elements.first().dri)
                        divergentGroup(
                            ContentDivergentGroup.GroupID(name),
                            elements.map { it.dri }.toSet(),
                            kind = ContentKind.Symbol
                        ) {
                            elements.map {
                                instance(setOf(it.dri), it.sourceSets.toSet()) {
                                    divergent {
                                        group(kind = ContentKind.Symbol) {
                                            +buildSignature(it)
                                        }
                                    }
                                    after {
                                        group(kind = ContentKind.BriefComment) {
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
    }


    protected open fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()
}
