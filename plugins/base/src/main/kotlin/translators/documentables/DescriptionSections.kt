package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.WithScope
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.orEmpty
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentStyle
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal const val KDOC_TAG_HEADER_LEVEL = 4

private val unnamedTagsExceptions: Set<KClass<out TagWrapper>> =
    setOf(Property::class, Description::class, Constructor::class, Param::class, See::class)

internal fun PageContentBuilder.DocumentableContentBuilder.descriptionSectionContent(
    documentable: Documentable,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
) {
    val descriptions = documentable.descriptions
    if (descriptions.any { it.value.root.children.isNotEmpty() }) {
        sourceSets.forEach { sourceSet ->
            descriptions[sourceSet]?.also {
                group(sourceSets = setOf(sourceSet), styles = emptySet()) {
                    comment(it.root)
                }
            }
        }
    }
}

/**
 * Custom tags are tags which are not part of the [KDoc specification](https://kotlinlang.org/docs/kotlin-doc.html). For instance, a user-defined tag
 * which is specific to the user's code base would be considered a custom tag.
 *
 * For details, see [CustomTagContentProvider]
 */
internal fun PageContentBuilder.DocumentableContentBuilder.customTagSectionContent(
    documentable: Documentable,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
    customTagContentProviders: List<CustomTagContentProvider>,
) {
    val customTags = documentable.customTags
    if (customTags.isEmpty()) return

    sourceSets.forEach { sourceSet ->
        customTags.forEach { (_, sourceSetTag) ->
            sourceSetTag[sourceSet]?.let { tag ->
                customTagContentProviders.filter { it.isApplicable(tag) }.forEach { provider ->
                    group(sourceSets = setOf(sourceSet), styles = setOf(ContentStyle.KDocTag)) {
                        with(provider) {
                            contentForDescription(sourceSet, tag)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tags in KDoc are used in form of "@tag name value".
 * This function handles tags that have only value parameter without name.
 * List of such tags: `@return`, `@author`, `@since`, `@receiver`
 */
internal fun PageContentBuilder.DocumentableContentBuilder.unnamedTagSectionContent(
    documentable: Documentable,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
    toHeaderString: TagWrapper.() -> String,
) {
    val unnamedTags = documentable.groupedTags
        .filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in unnamedTagsExceptions }
        .values.flatten().groupBy { it.first }
        .mapValues { it.value.map { it.second } }
        .takeIf { it.isNotEmpty() } ?: return

    sourceSets.forEach { sourceSet ->
        unnamedTags[sourceSet]?.let { tags ->
            if (tags.isNotEmpty()) {
                tags.groupBy { it::class }.forEach { (_, sameCategoryTags) ->
                    group(sourceSets = setOf(sourceSet), styles = setOf(ContentStyle.KDocTag)) {
                        header(
                            level = KDOC_TAG_HEADER_LEVEL,
                            text = sameCategoryTags.first().toHeaderString(),
                            styles = setOf()
                        )
                        sameCategoryTags.forEach { comment(it.root, styles = setOf()) }
                    }
                }
            }
        }
    }
}


internal fun PageContentBuilder.DocumentableContentBuilder.paramsSectionContent(tags: GroupedTags) {
    val params = tags.withTypeNamed<Param>()
    if (params.isEmpty()) return

    val availableSourceSets = params.availableSourceSets()
    tableSectionContentBlock(
        blockName = "Parameters",
        kind = ContentKind.Parameters,
        sourceSets = availableSourceSets
    ) {
        availableSourceSets.forEach { sourceSet ->
            val possibleFallbacks = availableSourceSets.getPossibleFallback(sourceSet)
            params.mapNotNull { (_, param) ->
                (param[sourceSet] ?: param.fallback(possibleFallbacks))?.let {
                    row(sourceSets = setOf(sourceSet), kind = ContentKind.Parameters) {
                        text(
                            it.name,
                            kind = ContentKind.Parameters,
                            styles = mainStyles + setOf(ContentStyle.RowTitle, TextStyle.Underlined)
                        )
                        if (it.isNotEmpty()) {
                            comment(it.root)
                        }
                    }
                }
            }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.seeAlsoSectionContent(tags: GroupedTags) {
    val seeAlsoTags = tags.withTypeNamed<See>()
    if (seeAlsoTags.isEmpty()) return

    val availableSourceSets = seeAlsoTags.availableSourceSets()
    tableSectionContentBlock(
        blockName = "See also",
        kind = ContentKind.Comment,
        sourceSets = availableSourceSets
    ) {
        availableSourceSets.forEach { sourceSet ->
            val possibleFallbacks = availableSourceSets.getPossibleFallback(sourceSet)
            seeAlsoTags.forEach { (_, see) ->
                (see[sourceSet] ?: see.fallback(possibleFallbacks))?.let { seeTag ->
                    row(
                        sourceSets = setOf(sourceSet),
                        kind = ContentKind.Comment
                    ) {
                        seeTag.address?.let { dri ->
                            link(
                                text = seeTag.name.removePrefix("${dri.packageName}."),
                                address = dri,
                                kind = ContentKind.Comment,
                                styles = mainStyles + ContentStyle.RowTitle
                            )
                        } ?: text(
                            text = seeTag.name,
                            kind = ContentKind.Comment,
                            styles = mainStyles + ContentStyle.RowTitle
                        )
                        if (seeTag.isNotEmpty()) {
                            comment(seeTag.root)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Used for multi-value tags (e.g. params) when values are missed on some platforms.
 * It this case description is inherited from parent platform.
 * E.g. if param hasn't description in JVM, the description is taken from common.
 */
private fun Set<DokkaConfiguration.DokkaSourceSet>.getPossibleFallback(sourceSet: DokkaConfiguration.DokkaSourceSet) =
    this.filter { it.sourceSetID in sourceSet.dependentSourceSets }

private fun <V> Map<DokkaConfiguration.DokkaSourceSet, V>.fallback(sourceSets: List<DokkaConfiguration.DokkaSourceSet>): V? =
    sourceSets.firstOrNull { it in this.keys }.let { this[it] }

internal fun PageContentBuilder.DocumentableContentBuilder.throwsSectionContent(tags: GroupedTags) {
    val throwsTags = tags.withTypeNamed<Throws>()
    if (throwsTags.isEmpty()) return

    val availableSourceSets = throwsTags.availableSourceSets()
    tableSectionContentBlock(
        blockName = "Throws",
        kind = ContentKind.Main,
        sourceSets = availableSourceSets
    ) {
        throwsTags.forEach { (throwsName, throwsPerSourceSet) ->
            throwsPerSourceSet.forEach { (sourceSet, throws) ->
                row(sourceSets = setOf(sourceSet)) {
                    group(styles = mainStyles + ContentStyle.RowTitle) {
                        throws.exceptionAddress?.let {
                            val className = it.takeIf { it.target is PointingToDeclaration }?.classNames
                            link(text = className ?: throwsName, address = it)
                        } ?: text(throwsName)
                    }
                    if (throws.isNotEmpty()) {
                        comment(throws.root)
                    }
                }
            }
        }
    }
}

private fun TagWrapper.isNotEmpty() = this.children.isNotEmpty()

internal fun PageContentBuilder.DocumentableContentBuilder.samplesSectionContent(tags: GroupedTags) {
    val samples = tags.withTypeNamed<Sample>()
    if (samples.isEmpty()) return

    val availableSourceSets = samples.availableSourceSets()

    header(KDOC_TAG_HEADER_LEVEL, "Samples", kind = ContentKind.Sample, sourceSets = availableSourceSets)
    availableSourceSets.forEach { sourceSet ->
        group(
            sourceSets = setOf(sourceSet),
            kind = ContentKind.Sample,
            styles = setOf(TextStyle.Monospace, ContentStyle.RunnableSample),
        ) {
            samples.filter { it.value.isEmpty() || sourceSet in it.value }
                .forEach { text(text = it.key, sourceSets = setOf(sourceSet)) }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.inheritorsSectionContent(
    documentable: Documentable,
    logger: DokkaLogger,
) {
    val inheritors = if (documentable is WithScope) documentable.inheritors() else return
    if (inheritors.values.none()) return

    // split content section for the case:
    // parent is in the shared source set (without expect-actual) and inheritor is in the platform code
    if (documentable.isDefinedInSharedSourceSetOnly(inheritors.keys.toSet()))
        sharedSourceSetOnlyInheritorsSectionContent(inheritors, logger)
    else
        multiplatformInheritorsSectionContent(documentable, inheritors, logger)
}

private fun WithScope.inheritors() = safeAs<WithExtraProperties<Documentable>>()
    ?.let { it.extra[InheritorsInfo] }
    ?.let { inheritors -> inheritors.value.filter { it.value.isNotEmpty() } }
    .orEmpty()

/**
 * Detect that documentable is located only in the shared code without expect-actuals
 * Value of `analysisPlatform` will be [Platform.common] in cases if a source set shared between 2 different platforms.
 * But if it shared between 2 same platforms (e.g. jvm("awt") and jvm("android"))
 * then the source set will be still marked as jvm platform.
 *
 * So, we also try to check if any of inheritors source sets depends on current documentable source set.
 * that will mean that the source set is shared.
 */
private fun Documentable.isDefinedInSharedSourceSetOnly(inheritorsSourceSets: Set<DokkaConfiguration.DokkaSourceSet>) =
    sourceSets.size == 1 &&
            (sourceSets.first().analysisPlatform == Platform.common
                    || sourceSets.first().hasDependentSourceSet(inheritorsSourceSets))

private fun DokkaConfiguration.DokkaSourceSet.hasDependentSourceSet(
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
) =
    sourceSets.any { sourceSet -> sourceSet.dependentSourceSets.any { it == this.sourceSetID } }

private fun PageContentBuilder.DocumentableContentBuilder.multiplatformInheritorsSectionContent(
    documentable: Documentable,
    inheritors: Map<DokkaConfiguration.DokkaSourceSet, List<DRI>>,
    logger: DokkaLogger,
) {
    // intersect is used for removing duplication in case of merged classlikes from different platforms
    val availableSourceSets = inheritors.keys.toSet().intersect(documentable.sourceSets)

    tableSectionContentBlock(
        blockName = "Inheritors",
        kind = ContentKind.Inheritors,
        sourceSets = availableSourceSets
    ) {
        availableSourceSets.forEach { sourceSet ->
            inheritors[sourceSet]?.forEach { classlike: DRI ->
                inheritorRow(classlike, logger, sourceSet)
            }
        }
    }
}

private fun PageContentBuilder.DocumentableContentBuilder.sharedSourceSetOnlyInheritorsSectionContent(
    inheritors: Map<DokkaConfiguration.DokkaSourceSet, List<DRI>>,
    logger: DokkaLogger,
) {
    val uniqueInheritors = inheritors.values.flatten().toSet()
    tableSectionContentBlock(
        blockName = "Inheritors",
        kind = ContentKind.Inheritors,
    ) {
        uniqueInheritors.forEach { classlike ->
            inheritorRow(classlike, logger)
        }
    }
}

private fun PageContentBuilder.TableBuilder.inheritorRow(
    classlike: DRI, logger: DokkaLogger, sourceSet: DokkaConfiguration.DokkaSourceSet? = null,
) = row {
    link(
        text = classlike.friendlyClassName()
            ?: classlike.toString().also { logger.warn("No class name found for DRI $classlike") },
        address = classlike,
        sourceSets = sourceSet?.let { setOf(it) } ?: mainSourcesetData
    )
}

private fun PageContentBuilder.DocumentableContentBuilder.tableSectionContentBlock(
    blockName: String,
    kind: ContentKind,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet> = mainSourcesetData,
    body: PageContentBuilder.TableBuilder.() -> Unit,
) {
    header(KDOC_TAG_HEADER_LEVEL, text = blockName, kind = kind, sourceSets = sourceSets)
    table(
        kind = kind,
        sourceSets = sourceSets,
    ) {
        body()
    }
}

private fun DRI.friendlyClassName() = classNames?.substringAfterLast(".")

private fun <T> Map<String, SourceSetDependent<T>>.availableSourceSets() = values.flatMap { it.keys }.toSet()
