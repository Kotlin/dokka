package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.documentables.CallableExtensions
import org.jetbrains.dokka.base.transformers.documentables.ClashingDriIdentifier
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.base.utils.canonicalAlphabeticalOrder
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.reflect.KClass

internal typealias GroupedTags = Map<KClass<out TagWrapper>, List<Pair<DokkaSourceSet?, TagWrapper>>>

open class DefaultPageCreator(
    configuration: DokkaBaseConfiguration?,
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger,
    val customTagContentProviders: List<CustomTagContentProvider> = emptyList()
) {
    protected open val contentBuilder = PageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    protected val mergeImplicitExpectActualDeclarations =
        configuration?.mergeImplicitExpectActualDeclarations
            ?: DokkaBaseConfiguration.mergeImplicitExpectActualDeclarationsDefault

    protected val separateInheritedMembers =
        configuration?.separateInheritedMembers ?: DokkaBaseConfiguration.separateInheritedMembersDefault

    open fun pageForModule(m: DModule): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "<root>" }, contentForModule(m), listOf(m), m.packages.map(::pageForPackage))

    open fun pageForPackage(p: DPackage): PackagePageNode = PackagePageNode(
        p.name, contentForPackage(p), setOf(p.dri), listOf(p),
        if (mergeImplicitExpectActualDeclarations)
            p.classlikes.mergeClashingDocumentable().map(::pageForClasslikes) +
                    p.functions.mergeClashingDocumentable().map(::pageForFunctions) +
                    p.properties.mergeClashingDocumentable().map(::pageForProperties)
        else
            p.classlikes.renameClashingDocumentable().map(::pageForClasslike) +
                    p.functions.renameClashingDocumentable().map(::pageForFunction) +
                    p.properties.mapNotNull(::pageForProperty)
    )

    open fun pageForEnumEntry(e: DEnumEntry): ClasslikePageNode = pageForEnumEntries(listOf(e))

    open fun pageForClasslike(c: DClasslike): ClasslikePageNode = pageForClasslikes(listOf(c))

    open fun pageForEnumEntries(documentables: List<DEnumEntry>): ClasslikePageNode {
        val dri = documentables.dri.also {
            if (it.size != 1) {
                logger.error("Documentable dri should have the same one ${it.first()} inside the one page!")
            }
        }

        val classlikes = documentables.flatMap { it.classlikes }
        val functions = documentables.flatMap { it.filteredFunctions }
        val props = documentables.flatMap { it.filteredProperties }

        val childrenPages = if (mergeImplicitExpectActualDeclarations)
            functions.mergeClashingDocumentable().map(::pageForFunctions) +
                    props.mergeClashingDocumentable().map(::pageForProperties)
        else
            classlikes.renameClashingDocumentable().map(::pageForClasslike) +
                    functions.renameClashingDocumentable().map(::pageForFunction) +
                    props.renameClashingDocumentable().mapNotNull(::pageForProperty)

        return ClasslikePageNode(
            documentables.first().nameAfterClash(), contentForClasslikesAndEntries(documentables), dri, documentables,
            childrenPages
        )
    }

    open fun pageForClasslikes(documentables: List<DClasslike>): ClasslikePageNode {
        val dri = documentables.dri.also {
            if (it.size != 1) {
                logger.error("Documentable dri should have the same one ${it.first()} inside the one page!")
            }
        }

        val constructors =
            if (documentables.shouldRenderConstructors()) {
                documentables.flatMap { (it as? WithConstructors)?.constructors ?: emptyList() }
            } else {
                emptyList()
            }

        val classlikes = documentables.flatMap { it.classlikes }
        val functions = documentables.flatMap { it.filteredFunctions }
        val props = documentables.flatMap { it.filteredProperties }
        val entries = documentables.flatMap { if (it is DEnum) it.entries else emptyList() }

        val childrenPages = constructors.map(::pageForFunction) +
                if (mergeImplicitExpectActualDeclarations)
                    classlikes.mergeClashingDocumentable().map(::pageForClasslikes) +
                            functions.mergeClashingDocumentable().map(::pageForFunctions) +
                            props.mergeClashingDocumentable().map(::pageForProperties) +
                            entries.mergeClashingDocumentable().map(::pageForEnumEntries)
                else
                    classlikes.renameClashingDocumentable().map(::pageForClasslike) +
                            functions.renameClashingDocumentable().map(::pageForFunction) +
                            props.renameClashingDocumentable().mapNotNull(::pageForProperty) +
                            entries.renameClashingDocumentable().map(::pageForEnumEntry)


        return ClasslikePageNode(
            documentables.first().nameAfterClash(), contentForClasslikesAndEntries(documentables), dri, documentables,
            childrenPages
        )
    }

    private fun <T> T.toClashedName() where T : Documentable, T : WithExtraProperties<T> =
        (extra[ClashingDriIdentifier]?.value?.joinToString(", ", "[", "]") { it.displayName } ?: "") + name.orEmpty()

    private fun <T : Documentable> List<T>.renameClashingDocumentable(): List<T> =
        groupBy { it.dri }.values.flatMap { elements ->
            if (elements.size == 1) elements else elements.mapNotNull { element ->
                element.renameClashingDocumentable()
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Documentable> T.renameClashingDocumentable(): T? = when (this) {
        is DClass -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        is DObject -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        is DAnnotation -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        is DInterface -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        is DEnum -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        is DFunction -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        is DProperty -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        is DTypeAlias -> copy(extra = this.extra + DriClashAwareName(this.toClashedName()))
        else -> null
    } as? T?

    private fun <T : Documentable> List<T>.mergeClashingDocumentable(): List<List<T>> =
        groupBy { it.dri }.values.toList()

    open fun pageForFunction(f: DFunction) =
        MemberPageNode(f.nameAfterClash(), contentForFunction(f), setOf(f.dri), listOf(f))

    open fun pageForFunctions(fs: List<DFunction>): MemberPageNode {
        val dri = fs.dri.also {
            if (it.size != 1) {
                logger.error("Function dri should have the same one ${it.first()} inside the one page!")
            }
        }
        return MemberPageNode(fs.first().nameAfterClash(), contentForMembers(fs), dri, fs)
    }

    open fun pageForProperty(p: DProperty): MemberPageNode? =
        MemberPageNode(p.nameAfterClash(), contentForProperty(p), setOf(p.dri), listOf(p))

    open fun pageForProperties(ps: List<DProperty>): MemberPageNode {
        val dri = ps.dri.also {
            if (it.size != 1) {
                logger.error("Property dri should have the same one ${it.first()} inside the one page!")
            }
        }
        return MemberPageNode(ps.first().nameAfterClash(), contentForMembers(ps), dri, ps)
    }

    private fun <T> T.isInherited(): Boolean where T : Documentable, T : WithExtraProperties<T> =
        sourceSets.all { sourceSet -> extra[InheritedMember]?.isInherited(sourceSet) == true }

    private val WithScope.filteredFunctions: List<DFunction>
        get() = functions.filterNot { it.isInherited() }

    private val WithScope.filteredProperties: List<DProperty>
        get() = properties.filterNot { it.isInherited() }

    private fun Collection<Documentable>.splitPropsAndFuns(): Pair<List<DProperty>, List<DFunction>> {
        val first = ArrayList<DProperty>()
        val second = ArrayList<DFunction>()
        for (element in this) {
            when (element) {
                is DProperty -> first.add(element)
                is DFunction -> second.add(element)
                else -> throw IllegalStateException("Expected only properties or functions")
            }
        }
        return Pair(first, second)
    }

    private fun <T> Collection<T>.splitInheritedExtension(dri: Set<DRI>): Pair<List<T>, List<T>> where T : Callable, T : WithExtraProperties<T> =
        partition { it.receiver?.dri !in dri }

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
                documentations.first()?.let { firstParagraphComment(kind = ContentKind.Comment, content = it.root) }
            }
        }
    }

    protected open fun contentForPackage(p: DPackage) = contentBuilder.contentFor(p) {
        group(kind = ContentKind.Cover) {
            cover("Package-level declarations")
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
        val contentTabsExtra = ContentTabsExtra(
            listOfNotNull(
                ContentTab(
                    ContentText(
                        "Types",
                        DCI(mainDRI, ContentKind.Main),
                        sourceSets = mainSourcesetData.toDisplaySourceSets()
                    ),
                    listOf(
                        BasicToggleableContentType.TYPE,
                    )
                ),
                if (p.functions.isEmpty()) null else ContentTab(
                    ContentText(
                        "Functions",
                        DCI(mainDRI, ContentKind.Main),
                        sourceSets = mainSourcesetData.toDisplaySourceSets()
                    ),
                    listOf(
                        BasicToggleableContentType.FUNCTION,
                        BasicToggleableContentType.EXTENSION,
                    )
                ),
                if (p.properties.isEmpty()) null else ContentTab(
                    ContentText(
                        "Properties",
                        DCI(mainDRI, ContentKind.Main),
                        sourceSets = mainSourcesetData.toDisplaySourceSets()
                    ),
                    listOf(
                        BasicToggleableContentType.PROPERTY,
                        BasicToggleableContentType.EXTENSION,
                    )
                )

            )
        )
        group(styles = setOf(ContentStyle.TabbedContent), extra = mainExtra + contentTabsExtra) {
            +contentForScope(p, p.dri, p.sourceSets)
        }
    }

    protected open fun contentForScopes(
        scopes: List<WithScope>,
        sourceSets: Set<DokkaSourceSet>,
        extensions: List<Documentable> = emptyList(),
        isClasslike: Boolean = false
    ): ContentGroup {
        val types = scopes.flatMap { it.classlikes } + scopes.filterIsInstance<DPackage>().flatMap { it.typealiases }
        return contentForScope(
            @Suppress("UNCHECKED_CAST")
            (scopes as List<Documentable>).dri,
            sourceSets,
            types,
            scopes.flatMap { it.functions },
            scopes.flatMap { it.properties },
            extensions,
            isClasslike
        )
    }

    protected open fun contentForScope(
        s: WithScope,
        dri: DRI,
        sourceSets: Set<DokkaSourceSet>,
        isClasslike: Boolean = false
    ): ContentGroup {
        val types = listOf(
            s.classlikes,
            (s as? DPackage)?.typealiases ?: emptyList()
        ).flatten()
        return contentForScope(setOf(dri), sourceSets, types, s.functions, s.properties, emptyList())
    }

    protected open fun contentForScope(
        dri: Set<DRI>,
        sourceSets: Set<DokkaSourceSet>,
        types: List<Documentable>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        extensions: List<Documentable>,
        isClasslike: Boolean = false
    ) = contentBuilder.contentFor(dri, sourceSets) {
        divergentBlock(
            "Types",
            types,
            ContentKind.Classlikes,
            extra = mainExtra + ToggleableContentTypeExtra(BasicToggleableContentType.TYPE),
            isVisibleHeader = isClasslike
        )
        val (extensionProps, extensionFuns) = extensions.splitPropsAndFuns()
        if (separateInheritedMembers) {
            val (inheritedFunctions, memberFunctions) = functions.splitInherited()
            val (inheritedProperties, memberProperties) = properties.splitInherited()

            val (inheritedExtensionFunctions, extensionFunctions) = extensionFuns.splitInheritedExtension(dri)
            val (inheritedExtensionProperties, extensionProperties) = extensionProps.splitInheritedExtension(dri)
            propertiesBlock(
                "Properties",
                BasicToggleableContentType.PROPERTY,
                memberProperties + extensionProperties,
                sourceSets,
                isVisibleHeader = isClasslike
            )
            propertiesBlock(
                "Inherited properties",
                BasicToggleableContentType.PROPERTY,
                inheritedProperties + inheritedExtensionProperties,
                sourceSets,
                isVisibleHeader = isClasslike
            )
            functionsBlock("Functions", BasicToggleableContentType.FUNCTION, memberFunctions + extensionFunctions, isVisibleHeader = isClasslike)
            functionsBlock(
                "Inherited functions",
                BasicToggleableContentType.FUNCTION,
                inheritedFunctions + inheritedExtensionFunctions,
                isVisibleHeader = isClasslike
            )
        } else {
            functionsBlock("Functions", BasicToggleableContentType.FUNCTION, functions + extensionFuns, isVisibleHeader = isClasslike)
            propertiesBlock(
                "Properties",
                BasicToggleableContentType.PROPERTY,
                properties + extensionProps,
                sourceSets,
                isVisibleHeader = isClasslike
            )
        }
    }

    private fun Iterable<DFunction>.sorted() =
        sortedWith(compareBy({ it.name }, { it.parameters.size }, { it.dri.toString() }))

    /**
     * @param documentables a list of [DClasslike] and [DEnumEntry] with the same dri in different sourceSets
     */
    protected open fun contentForClasslikesAndEntries(documentables: List<Documentable>): ContentGroup =
        contentBuilder.contentFor(documentables.dri, documentables.sourceSets) {
            val classlikes = documentables.filterIsInstance<DClasslike>()

            @Suppress("UNCHECKED_CAST")
            val extensions = (classlikes as List<WithExtraProperties<DClasslike>>).flatMap {
                it.extra[CallableExtensions]?.extensions
                    ?.filterIsInstance<Documentable>().orEmpty()
            }
                .distinctBy { it.sourceSets to it.dri } // [Documentable] has expensive equals/hashCode at the moment, see #2620

            // Extensions are added to sourceSets since they can be placed outside the sourceSets from classlike
            // Example would be an Interface in common and extension function in jvm
            group(kind = ContentKind.Cover, sourceSets = mainSourcesetData + extensions.sourceSets) {
                cover(documentables.first().name.orEmpty())
                sourceSetDependentHint(documentables.dri, documentables.sourceSets) {
                    documentables.forEach {
                        +buildSignature(it)
                        +contentForDescription(it)
                    }
                }
            }
            val csEnum = classlikes.filterIsInstance<DEnum>()
            val csWithConstructor = classlikes.filterIsInstance<WithConstructors>()
            val scopes = documentables.filterIsInstance<WithScope>()
            val constructorsToDocumented = csWithConstructor.flatMap { it.constructors }

            val containsAnyConstructor = constructorsToDocumented.isNotEmpty() && documentables.shouldRenderConstructors()
            val containsAnyMember =
                containsAnyConstructor || scopes.any { it.classlikes.isNotEmpty() || it.functions.isNotEmpty() || it.properties.isNotEmpty() }
            val contentTabsExtra = ContentTabsExtra(
                listOfNotNull(
                    if (!containsAnyMember) null else ContentTab(
                        ContentText(
                            "Members",
                            DCI(mainDRI, ContentKind.Main),
                            sourceSets = (mainSourcesetData + extensions.sourceSets).toDisplaySourceSets()
                        ),
                        listOf(
                            BasicToggleableContentType.CONSTRUCTOR,
                            BasicToggleableContentType.TYPE,
                            BasicToggleableContentType.FUNCTION,
                            BasicToggleableContentType.INHERITED_FUNCTION,
                            BasicToggleableContentType.PROPERTY,
                            BasicToggleableContentType.INHERITED_PROPERTY
                        )
                    ),
                    if (extensions.isEmpty()) null else ContentTab(
                        ContentText(
                            "Members & Extensions",
                            DCI(mainDRI, ContentKind.Main),
                            sourceSets = (mainSourcesetData + extensions.sourceSets).toDisplaySourceSets()
                        ),
                        listOf(
                            BasicToggleableContentType.CONSTRUCTOR,
                            BasicToggleableContentType.TYPE,
                            BasicToggleableContentType.FUNCTION,
                            BasicToggleableContentType.INHERITED_FUNCTION,
                            BasicToggleableContentType.PROPERTY,
                            BasicToggleableContentType.INHERITED_PROPERTY,
                            BasicToggleableContentType.EXTENSION,
                        )
                    ),
                    if(csEnum.isEmpty()) null else ContentTab(
                        ContentText(
                            "Entries",
                            DCI(mainDRI, ContentKind.Main),
                            sourceSets = (mainSourcesetData + extensions.sourceSets).toDisplaySourceSets()
                        ),
                        listOf(
                            BasicToggleableContentType.ENTRY
                        )
                    )
                )
            )
            group(
                styles = setOf(ContentStyle.TabbedContent),
                sourceSets = mainSourcesetData + extensions.sourceSets,
                extra = mainExtra + contentTabsExtra
            ) {
                if (constructorsToDocumented.isNotEmpty() && documentables.shouldRenderConstructors()) {
                    +contentForConstructors(constructorsToDocumented, classlikes.dri, classlikes.sourceSets)
                }
                if (csEnum.isNotEmpty()) {
                    +contentForEntries(csEnum.flatMap { it.entries }, csEnum.dri, csEnum.sourceSets)
                }
                +contentForScopes(scopes, documentables.sourceSets, extensions, isClasslike = true)
            }
        }
    protected open fun contentForConstructors(
        constructorsToDocumented: List<DFunction>,
        dri: Set<DRI>,
        sourceSets: Set<DokkaSourceSet>
    ) = contentBuilder.contentFor(dri, sourceSets) {
        multiBlock(
            "Constructors",
            2,
            ContentKind.Constructors,
            constructorsToDocumented.groupBy { it.name }
                .map { (_, v) -> v.first().name to v },
            @Suppress("UNCHECKED_CAST")
            (constructorsToDocumented as List<Documentable>).sourceSets,
            needsAnchors = true,
            isVisibleHeader = true,
            extra = PropertyContainer.empty<ContentNode>() + ToggleableContentTypeExtra(
                BasicToggleableContentType.CONSTRUCTOR
            ),
        ) { key, ds ->
            link(key, ds.first().dri, kind = ContentKind.Main, styles = setOf(ContentStyle.RowTitle))
            sourceSetDependentHint(
                ds.dri,
                ds.sourceSets,
                kind = ContentKind.SourceSetDependentHint,
                styles = emptySet(),
                extra = PropertyContainer.empty<ContentNode>()
            ) {
                ds.forEach {
                    +buildSignature(it)
                    contentForBrief(it)
                }
            }
        }
    }

    protected open fun contentForEntries(
        entries: List<DEnumEntry>,
        dri: Set<DRI>,
        sourceSets: Set<DokkaSourceSet>
    ) = contentBuilder.contentFor(dri, sourceSets) {
        multiBlock(
            "Entries",
            2,
            ContentKind.Classlikes,
            entries.groupBy { it.name }.toList(),
            entries.sourceSets,
            needsSorting = false,
            needsAnchors = true,
            extra = mainExtra + ToggleableContentTypeExtra(BasicToggleableContentType.ENTRY),
            styles = emptySet()
        ) { key, ds ->
            link(key, ds.first().dri)
            sourceSetDependentHint(
                ds.dri,
                ds.sourceSets,
                kind = ContentKind.SourceSetDependentHint,
                extra = PropertyContainer.empty<ContentNode>()
            ) {
                ds.forEach {
                    +buildSignature(it)
                    contentForBrief(it)
                }
            }
        }
    }

    // Annotations might have constructors to substitute reflection invocations
    // and for internal/compiler purposes, but they are not expected to be documented
    // and instantiated directly under normal circumstances, so constructors should not be rendered.
    private fun List<Documentable>.shouldRenderConstructors() = !this.any { it is DAnnotation }

    protected open fun contentForDescription(
        d: Documentable
    ): List<ContentNode> {
        val sourceSets = d.sourceSets.toSet()
        val tags = d.groupedTags

        return contentBuilder.contentFor(d) {
            deprecatedSectionContent(d, sourceSets)

            descriptionSectionContent(d, sourceSets)
            customTagSectionContent(d, sourceSets, customTagContentProviders)
            unnamedTagSectionContent(d, sourceSets) { toHeaderString() }

            paramsSectionContent(tags)
            seeAlsoSectionContent(tags)
            throwsSectionContent(tags)
            samplesSectionContent(tags)

            inheritorsSectionContent(d, logger)
        }.children
    }

    protected open fun DocumentableContentBuilder.contentForBrief(
        documentable: Documentable,
        extra: PropertyContainer<ContentNode> = mainExtra
    ) {
        documentable.sourceSets.forEach { sourceSet ->
            documentable.documentation[sourceSet]?.let {
                /*
                    Get description or a tag that holds documentation.
                    This tag can be either property or constructor but constructor tags are handled already in analysis so we
                    only need to keep an eye on property

                    We purposefully ignore all other tags as they should not be visible in brief
                 */
                it.firstMemberOfTypeOrNull<Description>() ?: it.firstMemberOfTypeOrNull<Property>()
                    .takeIf { documentable is DProperty }
            }?.let {
                group(sourceSets = setOf(sourceSet), kind = ContentKind.BriefComment, extra = extra) {
                    if (documentable.hasSeparatePage) createBriefComment(documentable, sourceSet, it)
                    else comment(it.root)
                }
            }
        }
    }

    private fun DocumentableContentBuilder.createBriefComment(
        documentable: Documentable,
        sourceSet: DokkaSourceSet,
        tag: TagWrapper
    ) {
        (documentable as? WithSources)?.documentableLanguage(sourceSet)?.let {
            when (it) {
                DocumentableLanguage.KOTLIN -> firstParagraphComment(tag.root)
                DocumentableLanguage.JAVA -> firstSentenceComment(tag.root)
            }
        } ?: firstParagraphComment(tag.root)
    }

    protected open fun contentForFunction(f: DFunction) = contentForMember(f)

    protected open fun contentForProperty(p: DProperty) = contentForMember(p)

    protected open fun contentForMember(d: Documentable) = contentForMembers(listOf(d))

    protected open fun contentForMembers(doumentables: List<Documentable>) =
        contentBuilder.contentFor(doumentables.dri, doumentables.sourceSets) {
            group(kind = ContentKind.Cover) {
                cover(doumentables.first().name.orEmpty())
            }
            divergentGroup(ContentDivergentGroup.GroupID("member")) {
                doumentables.forEach { d ->
                    instance(setOf(d.dri), d.sourceSets) {
                        divergent {
                            +buildSignature(d)
                        }
                        after {
                            +contentForDescription(d)
                        }
                    }
                }
            }
        }

    private fun DocumentableContentBuilder.functionsBlock(
        name: String,
        toggleableContentType: ToggleableContentType,
        list: Collection<DFunction>,
        isVisibleHeader: Boolean = false
    ) = divergentBlock(
        name,
        list.sorted(),
        ContentKind.Functions,
        extra = mainExtra + ToggleableContentTypeExtra(toggleableContentType),
        isVisibleHeader = isVisibleHeader
    )

    private fun DocumentableContentBuilder.propertiesBlock(
        name: String,
        toggleableContentType: ToggleableContentType,
        list: Collection<DProperty>,
        sourceSets: Set<DokkaSourceSet>,
        isVisibleHeader: Boolean = false
    ) {
        val groupedElements = list.groupBy { Pair(it.name, it.isExtension()) }.toList()
        val sortedGroupedElements =
            groupedElements.sortedWith(compareBy<Pair<Pair<String, Boolean>, List<DProperty>>, String>(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it.first.first }.thenBy { it.first.second })

        multiBlock(
            name,
            2,
            ContentKind.Properties,
            sortedGroupedElements.map { it.first.first to it.second },
            sourceSets,
            needsAnchors = true,
            extra = mainExtra + ToggleableContentTypeExtra(toggleableContentType),
            headers = listOf(
                headers("Name", "Summary")
            ),
            isVisibleHeader = isVisibleHeader
        ) { key, props ->
            val extra =
                if (props.all { it.isExtension() }) mainExtra + ToggleableContentTypeExtra(BasicToggleableContentType.EXTENSION) else mainExtra
            link(
                text = key,
                address = props.first().dri,
                kind = ContentKind.Main,
                styles = setOf(ContentStyle.RowTitle),
                extra = extra
            )
            sourceSetDependentHint(
                props.dri,
                props.sourceSets,
                kind = ContentKind.SourceSetDependentHint,
                extra = extra
            ) {

                props.forEach {
                    +buildSignature(it)
                    contentForBrief(it)
                    contentForCustomTagsBrief(it)
                }
            }
        }
    }

    private val groupKeyComparator: Comparator<Map.Entry<String?, *>> =
        compareBy(nullsFirst(canonicalAlphabeticalOrder)) { it.key }

    protected open fun DocumentableContentBuilder.divergentBlock(
        name: String,
        collection: Collection<Documentable>,
        kind: ContentKind,
        extra: PropertyContainer<ContentNode> = mainExtra,
        isVisibleHeader: Boolean = false
    ) {
        if (collection.any()) {
            group(extra = extra) {
                // corner case
                val onlyExtensions = collection.all { it.isExtension() }
                val headerExtra = if (onlyExtensions)
                    extra + ToggleableContentTypeExtra(BasicToggleableContentType.EXTENSION)
                else
                    extra
                val headerExtraWithVisibility = if (isVisibleHeader) headerExtra else headerExtra + HtmlInvisibleExtra
                header(2, name, kind = kind, extra = headerExtraWithVisibility) { }

                table(kind, extra = extra, styles = emptySet()) {
                    header {
                        group { text("Name") }
                        group { text("Summary") }
                    }
                    collection
                        .groupBy {
                            Pair(
                                it.name,
                                it.isExtension()
                            )
                        } // This groupBy should probably use LocationProvider
                        // This hacks displaying actual typealias signatures along classlike ones
                        .mapValues { if (it.value.any { it is DClasslike }) it.value.filter { it !is DTypeAlias } else it.value }
                        .toSortedMap(compareBy<Pair<String?, Boolean>, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it.first }.thenBy { it.second })
                        .forEach { (elementNameAndIsExtension, elements) -> // This groupBy should probably use LocationProvider
                            val elementName = elementNameAndIsExtension.first
                            val isExtension = elementNameAndIsExtension.second
                            val rowExtra =
                                if (isExtension) extra + ToggleableContentTypeExtra(BasicToggleableContentType.EXTENSION) else extra

                            row(
                                dri = elements.map { it.dri }.toSet(),
                                sourceSets = elements.flatMap { it.sourceSets }.toSet(),
                                kind = kind,
                                styles = emptySet(),
                                extra = elementName?.let { name -> rowExtra + SymbolAnchorHint(name, kind) } ?: rowExtra
                            ) {
                                link(
                                    text = elementName.orEmpty(),
                                    address = elements.first().dri,
                                    kind = kind,
                                    styles = setOf(ContentStyle.RowTitle),
                                    sourceSets = elements.sourceSets.toSet(),
                                    extra = extra
                                )
                                divergentGroup(
                                    ContentDivergentGroup.GroupID(name),
                                    elements.map { it.dri }.toSet(),
                                    kind = kind,
                                    extra = extra
                                ) {
                                    elements.map { element ->
                                        instance(
                                            setOf(element.dri),
                                            element.sourceSets.toSet(),
                                            extra = PropertyContainer.withAll(
                                                SymbolAnchorHint(element.name ?: "", kind)
                                            )
                                        ) {
                                            divergent(extra = PropertyContainer.empty()) {
                                                group {
                                                    +buildSignature(element)
                                                }
                                            }
                                            after(
                                                extra = PropertyContainer.empty()
                                            ) {
                                                contentForBrief(element)
                                                contentForCustomTagsBrief(element)
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

    /**
     * Divergent elements, such as extensions for the same receiver, can have identical signatures
     * if they are declared in different places. If such elements are shown on the same page together,
     * they need to be rendered deterministically to have reproducible builds.
     *
     * For example, you can have three identical extensions, if they are declared as:
     * 1) top-level in package A
     * 2) top-level in package B
     * 3) inside a companion object in package A/B
     *
     * @see divergentBlock
     *
     * @param elements can contain types (annotation/class/interface/object/typealias), functions and properties
     * @return the original list if it has one or zero elements
     */
    private fun sortDivergentElementsDeterministically(elements: List<Documentable>): List<Documentable> =
        elements.takeIf { it.size > 1 } // the majority are single-element lists, but no real benchmarks done
            ?.sortedWith(divergentDocumentableComparator)
            ?: elements

    private fun DocumentableContentBuilder.contentForCustomTagsBrief(documentable: Documentable) {
        val customTags = documentable.customTags
        if (customTags.isEmpty()) return

        documentable.sourceSets.forEach { sourceSet ->
            customTags.forEach { (_, sourceSetTag) ->
                sourceSetTag[sourceSet]?.let { tag ->
                    customTagContentProviders.filter { it.isApplicable(tag) }.forEach { provider ->
                        with(provider) {
                            contentForBrief(sourceSet, tag)
                        }
                    }
                }
            }
        }
    }

    protected open fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()
}

fun Documentable.isExtension() = this is Callable && receiver != null

internal val List<Documentable>.sourceSets: Set<DokkaSourceSet>
    get() = flatMap { it.sourceSets }.toSet()

internal val List<Documentable>.dri: Set<DRI>
    get() = map { it.dri }.toSet()

internal val Documentable.groupedTags: GroupedTags
    get() = documentation.flatMap { (pd, doc) ->
        doc.children.map { pd to it }.toList()
    }.groupBy { it.second::class }

internal val Documentable.descriptions: SourceSetDependent<Description>
    get() = groupedTags.withTypeUnnamed()

internal val Documentable.customTags: Map<String, SourceSetDependent<CustomTagWrapper>>
    get() = groupedTags.withTypeNamed()

private val Documentable.hasSeparatePage: Boolean
    get() = this !is DTypeAlias

/**
 * @see DefaultPageCreator.sortDivergentElementsDeterministically for usage
 */
private val divergentDocumentableComparator =
    compareBy<Documentable, String?>(nullsLast()) { it.dri.packageName }
        .thenBy(nullsFirst()) { it.dri.classNames } // nullsFirst for top level to be first
        .thenBy(
            nullsLast(
                compareBy<Callable> { it.params.size }
                    .thenBy { it.signature() }
            )
        ) { it.dri.callable }

@Suppress("UNCHECKED_CAST")
private fun <T : Documentable> T.nameAfterClash(): String =
    ((this as? WithExtraProperties<out Documentable>)?.extra?.get(DriClashAwareName)?.value ?: name).orEmpty()

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T : TagWrapper> GroupedTags.withTypeUnnamed(): SourceSetDependent<T> =
    (this[T::class] as List<Pair<DokkaSourceSet, T>>?)?.toMap().orEmpty()

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T : NamedTagWrapper> GroupedTags.withTypeNamed(): Map<String, SourceSetDependent<T>> =
    (this[T::class] as List<Pair<DokkaSourceSet, T>>?)
        ?.groupByTo(linkedMapOf()) { it.second.name }
        ?.mapValues { (_, v) -> v.toMap() }
        .orEmpty()
