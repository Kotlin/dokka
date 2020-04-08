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
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

private typealias GroupedTags = Map<KClass<out TagWrapper>, List<Pair<PlatformData?, TagWrapper>>>

private val specialTags: Set<KClass<out TagWrapper>> =
    setOf(Property::class, Description::class, Constructor::class, Receiver::class, Param::class, See::class)


open class DefaultPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    protected open val contentBuilder = PageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    open fun pageForModule(m: DModuleView) =
        ModulePageNode(m.name.orEmpty().ifEmpty { "<root>" }, contentForModule(m), m, m.packages.map(::pageForPackage))

    open fun pageForPackage(p: DPackage): PackagePageNode = PackagePageNode(
        p.name, contentForPackage(p), setOf(p.dri), p,
        p.classlikes.map(::pageForClasslike) +
                p.functions.map(::pageForFunction) +
                p.typealiases.map(::pageForTypeAlias)
    )

    open fun pageForEnumEntry(e: DEnumEntry): ClasslikePageNode =
        ClasslikePageNode(
            e.name.orEmpty(), contentForEnumEntry(e), setOf(e.dri), e,
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

    protected open fun contentForModule(m: DModuleView) = contentBuilder.contentFor(m) {
        header(1) { text(m.name.orEmpty()) }
        block("Packages", 2, ContentKind.Packages, m.packages, m.platformData.toSet()) {
            link(it.name, it.dri)
        }
//        text("Index\n") TODO
//        text("Link to allpage here")
    }

    protected open fun contentForPackage(p: DPackage) = contentBuilder.contentFor(p) {
        group(p.dri, p.platformData.toSet(), ContentKind.Packages) {
            header(1) { text("Package ${p.name}") }
        }
        +contentForScope(p, p.dri, p.platformData)
        block("Type aliases", 2, ContentKind.TypeAliases, p.typealiases, p.platformData.toSet()) {
            link(it.name, it.dri)
            group {
                platformDependentHint(it.dri, it.platformData.toSet()) {
                    +buildSignature(it)
                }
                group(kind = ContentKind.BriefComment) {
                    text(it.briefDocumentation())
                }
            }
        }
    }

    protected open fun contentForScope(
        s: WithScope,
        dri: DRI,
        platformData: List<PlatformData>
    ) = contentBuilder.contentFor(s as Documentable) {
        block("Types", 2, ContentKind.Classlikes, s.classlikes, platformData.toSet()) {
            link(it.name.orEmpty(), it.dri)
            group {
                platformDependentHint(it.dri, it.platformData.toSet()) {
                    +buildSignature(it)
                }
                group(kind = ContentKind.BriefComment) {
                    text(it.briefDocumentation())
                }
            }
        }
        block("Functions", 2, ContentKind.Functions, s.functions, platformData.toSet()) {
            link(it.name, it.dri)
            group {
                platformDependentHint(it.dri, it.platformData.toSet()) {
                    +buildSignature(it)
                }
                group(kind = ContentKind.BriefComment) {
                    text(it.briefDocumentation())
                }
            }
        }
        block("Properties", 2, ContentKind.Properties, s.properties, platformData.toSet()) {
            link(it.name, it.dri)
            group {
                platformDependentHint(it.dri, it.platformData.toSet()) {
                    +buildSignature(it)
                }
                breakLine()
                group(kind = ContentKind.BriefComment) {
                    text(it.briefDocumentation())
                }
            }
        }
        (s as? WithExtraProperties<Documentable>)?.let { it.extra[InheritorsInfo] }?.let { inheritors ->
            val map = inheritors.value.map
            text("Subclasses:")
            platformDependentHint(dri = s.dri, platformData = map.keys) {
                map.keys.forEach { pd ->
                    linkTable(map[pd].orEmpty(), ContentKind.Classlikes, setOf(pd))
                }
            }
        }
    }

    protected open fun contentForEnumEntry(e: DEnumEntry) = contentBuilder.contentFor(e) {
        header(1) { text(e.name.orEmpty()) }
        +buildSignature(e)

        +contentForComments(e)

        +contentForScope(e, e.dri, e.platformData)
    }

    protected open fun contentForClasslike(c: DClasslike) = contentBuilder.contentFor(c) {
        group(c.dri, c.platformData.toSet(), ContentKind.Classlikes) {
            header(1) { text(c.name.orEmpty()) }
            platformDependentHint(c.dri, c.platformData.toSet()) {
                +buildSignature(c)
            }
        }
        breakLine()
        +contentForComments(c)

        if (c is WithConstructors) {
            block(
                "Constructors",
                2,
                ContentKind.Constructors,
                c.constructors.filter { it.extra[PrimaryConstructorExtra] == null },
                c.platformData.toSet()
            ) {
                link(it.name, it.dri)
                group {
                    platformDependentHint(it.dri, it.platformData.toSet()) {
                        +buildSignature(it)
                    }
                    group(kind = ContentKind.BriefComment) {
                        text(it.briefDocumentation())
                    }
                }
            }
        }
        if (c is DEnum) {
            block("Entries", 2, ContentKind.Classlikes, c.entries, c.platformData.toSet()) {
                link(it.name.orEmpty(), it.dri)
                group {
                    platformDependentHint(it.dri, it.platformData.toSet()) {
                        +buildSignature(it)
                    }
                    group(kind = ContentKind.BriefComment) {
                        text(it.briefDocumentation())
                    }
                }
            }
        }

        +contentForScope(c, c.dri, c.platformData)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : TagWrapper> GroupedTags.withTypeUnnamed(): PlatformDependent<T> =
        (this[T::class] as List<Pair<PlatformData, T>>?)
            ?.let { PlatformDependent.from(it) }.orEmpty()

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : NamedTagWrapper> GroupedTags.withTypeNamed(): Map<String, PlatformDependent<T>> =
        (this[T::class] as List<Pair<PlatformData, T>>?)
            ?.groupBy { it.second.name }
            ?.mapValues { (_, v) -> PlatformDependent.from(v) }
            .orEmpty()

    protected open fun contentForComments(
        d: Documentable
    ): List<ContentNode> {
        val tags: GroupedTags = d.documentation.allEntries.flatMap { (pd, doc) ->
            doc.children.asSequence().map { pd to it }
        }.groupBy { it.second::class }

        val platforms = d.platformData

        fun DocumentableContentBuilder.contentForDescription() {
            val description = tags.withTypeUnnamed<Description>()
            if (description.any { it.value.root.children.isNotEmpty() }) {
                platforms.forEach { platform ->
                    description.getOrExpect(platform)?.also {
                        group(platformData = setOf(platform)) {
                            comment(it.root)
                            breakLine()
                        }
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForParams() {
            val receiver = tags.withTypeUnnamed<Receiver>()
            val params = tags.withTypeNamed<Param>()

            if (params.isNotEmpty()) {
                header(4, kind = ContentKind.Parameters) { text("Parameters") }
                table(kind = ContentKind.Parameters) {
                    platforms.flatMap { platform ->
                        val receiverRow = receiver.getOrExpect(platform)?.let {
                            buildGroup(platformData = setOf(platform)) {
                                text("<receiver>")
                                comment(it.root)
                            }
                        }

                        val paramRows = params.mapNotNull { (_, param) ->
                            param.getOrExpect(platform)?.let {
                                buildGroup(platformData = setOf(platform)) {
                                    text(it.name)
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
            val seeAlsoTags = tags.withTypeNamed<See>()
            if(seeAlsoTags.isNotEmpty()) {
                header(4, kind = ContentKind.Comment) { text("See also") }
                table(kind = ContentKind.Comment) {
                    platforms.flatMap { platform ->
                        seeAlsoTags.mapNotNull { (_, see) ->
                            see.getOrExpect(platform)?.let {
                                buildGroup(platformData = setOf(platform)) {
                                    if (it.address != null) link(it.name, it.address!!)
                                    else text(it.name)
                                    comment(it.root)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun DocumentableContentBuilder.contentForUnnamedTags() {
            val unnamedTags: List<PlatformDependent<TagWrapper>> =
                tags.filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in specialTags }
                    .map { (_, v) -> PlatformDependent.from(v) }
            platforms.forEach { platform ->
                unnamedTags.forEach { pdTag ->
                    pdTag.getOrExpect(platform)?.also { tag ->
                        group(platformData = setOf(platform)) {
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
                platformDependentHint(platformData = platforms.toSet()) {
                    contentForDescription()
                    contentForParams()
                    contentForUnnamedTags()
                    contentForSeeAlso()
                }
            }
        }.children
    }

    protected open fun contentForFunction(f: DFunction) = contentBuilder.contentFor(f) {
        group(f.dri, f.platformData.toSet(), ContentKind.Functions) {
            header(1) { text(f.name) }
            platformDependentHint(f.dri, f.platformData.toSet()) {
                +buildSignature(f)
            }
        }
        +contentForComments(f)
    }

    protected open fun contentForTypeAlias(t: DTypeAlias) = contentBuilder.contentFor(t) {
        header(1) { text(t.name) }
        +buildSignature(t)
        +contentForComments(t)
    }

    protected open fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()

    //TODO: It isn't platform-aware and produces wrong docs Probably should use platformDependentHint
    protected open fun Documentable.briefDocumentation() = " "
//        documentation.values
//            .firstOrNull()
//            ?.children
//            ?.firstOrNull()
//            ?.root
//            ?.docTagSummary() ?: ""
}
