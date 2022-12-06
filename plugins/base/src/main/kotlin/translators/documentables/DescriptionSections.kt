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
import org.jetbrains.dokka.pages.SimpleAttr
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


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

internal fun PageContentBuilder.DocumentableContentBuilder.customTagSectionContent(
    documentable: Documentable,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
    customTagContentProviders: List<CustomTagContentProvider>,
) {
    val customTags = documentable.customTags ?: return

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

internal fun PageContentBuilder.DocumentableContentBuilder.unnamedTagSectionContent(
    documentable: Documentable,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
    toHeaderString: TagWrapper.() -> String,
) {
    val unnamedTags = documentable.groupedTags
        .filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in specialTags }
        .values.flatten().groupBy { it.first }.mapValues { it.value.map { it.second } }
    if (unnamedTags.isEmpty()) return
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
    val params = tags.withTypeNamed<Param>() ?: return

    val availableSourceSets = params.availableSourceSets()
    header(KDOC_TAG_HEADER_LEVEL, "Parameters", kind = ContentKind.Parameters, sourceSets = availableSourceSets)
    table(
        kind = ContentKind.Parameters,
        extra = mainExtra + SimpleAttr.header("Parameters"),
        sourceSets = availableSourceSets
    )
    {
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
    val seeAlsoTags = tags.withTypeNamed<See>() ?: return

    val availableSourceSets = seeAlsoTags.availableSourceSets()
    header(KDOC_TAG_HEADER_LEVEL, "See also", kind = ContentKind.Comment, sourceSets = availableSourceSets)

    table(
        kind = ContentKind.Comment,
        extra = mainExtra + SimpleAttr.header("See also"),
        sourceSets = availableSourceSets
    )
    {
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

internal fun PageContentBuilder.DocumentableContentBuilder.throwsSectionContent(tags: GroupedTags) {
    val throwsTags = tags.withTypeNamed<Throws>() ?: return
    val availableSourceSets = throwsTags.availableSourceSets()

    header(KDOC_TAG_HEADER_LEVEL, "Throws", sourceSets = availableSourceSets)
    table(
        kind = ContentKind.Main,
        sourceSets = availableSourceSets,
        extra = mainExtra + SimpleAttr.header("Throws")
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

internal fun PageContentBuilder.DocumentableContentBuilder.samplesSectionContent(tags: GroupedTags) {
    val samples = tags.withTypeNamed<Sample>() ?: return
    val availableSourceSets = samples.availableSourceSets()

    header(KDOC_TAG_HEADER_LEVEL, "Samples", kind = ContentKind.Sample, sourceSets = availableSourceSets)
    availableSourceSets.forEach { sourceSet ->
        group(
            sourceSets = setOf(sourceSet),
            kind = ContentKind.Sample,
            styles = setOf(TextStyle.Monospace, ContentStyle.RunnableSample),
            extra = mainExtra + SimpleAttr.header("Samples")
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
                    || sourceSets.first().hasDependentSourceSetInInheritors(inheritorsSourceSets))

private fun DokkaConfiguration.DokkaSourceSet.hasDependentSourceSetInInheritors(
    inheritorsSourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
) =
    inheritorsSourceSets.any { sourceSet -> sourceSet.dependentSourceSets.any { it == this.sourceSetID } }

private fun PageContentBuilder.DocumentableContentBuilder.multiplatformInheritorsSectionContent(
    documentable: Documentable,
    inheritors: Map<DokkaConfiguration.DokkaSourceSet, List<DRI>>,
    logger: DokkaLogger,
) {
    // intersect is used for removing duplication in case of merged classlikes from different platforms
    val availableSourceSets = inheritors.keys.toSet().intersect(documentable.sourceSets)

    header(KDOC_TAG_HEADER_LEVEL, "Inheritors", sourceSets = availableSourceSets)
    table(
        kind = ContentKind.Inheritors,
        sourceSets = availableSourceSets,
        extra = mainExtra + SimpleAttr.header("Inheritors")
    ) {
        availableSourceSets.forEach { sourceSet ->
            inheritors[sourceSet]?.forEach { classlike: DRI ->
                inheritorRow(classlike, logger, sourceSet)
            }
        }
    }
}

// `sourceSets` parameters is not applied on purpose
private fun PageContentBuilder.DocumentableContentBuilder.sharedSourceSetOnlyInheritorsSectionContent(
    inheritors: Map<DokkaConfiguration.DokkaSourceSet, List<DRI>>,
    logger: DokkaLogger,
) {
    val uniqueInheritors = inheritors.values.flatten().toSet()

    header(KDOC_TAG_HEADER_LEVEL, "Inheritors")
    table(
        kind = ContentKind.Inheritors,
        extra = mainExtra + SimpleAttr.header("Inheritors")
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

private fun WithScope.inheritors() = safeAs<WithExtraProperties<Documentable>>()
    ?.let { it.extra[InheritorsInfo] }
    ?.let { inheritors -> inheritors.value.filter { it.value.isNotEmpty() } }
    .orEmpty()

private fun DRI.friendlyClassName() = classNames?.substringAfterLast(".")

private fun TagWrapper.isNotEmpty() = this.children.isNotEmpty()

private fun <V> Map<DokkaConfiguration.DokkaSourceSet, V>.fallback(sourceSets: List<DokkaConfiguration.DokkaSourceSet>): V? =
    sourceSets.firstOrNull { it in this.keys }.let { this[it] }

/**
 * Used for multi-value tags (e.g. params) when values are missed on some platforms.
 * It this case description is inherited from parent platform.
 * E.g. if param hasn't description in JVM, the description is taken from common.
 */
private fun Set<DokkaConfiguration.DokkaSourceSet>.getPossibleFallback(sourceSet: DokkaConfiguration.DokkaSourceSet) =
    this.filter { it.sourceSetID in sourceSet.dependentSourceSets }

private val specialTags: Set<KClass<out TagWrapper>> =
    setOf(Property::class, Description::class, Constructor::class, Param::class, See::class)

private fun <T> Map<String, SourceSetDependent<T>>.availableSourceSets() = values.flatMap { it.keys }.toSet()


