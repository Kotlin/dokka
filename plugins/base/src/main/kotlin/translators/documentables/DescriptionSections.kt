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
        sourceSets.forEach { platform ->
            descriptions[platform]?.also {
                group(sourceSets = setOf(platform), styles = emptySet()) {
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

    sourceSets.forEach { platform ->
        customTags.forEach { (_, sourceSetTag) ->
            sourceSetTag[platform]?.let { tag ->
                customTagContentProviders.filter { it.isApplicable(tag) }.forEach { provider ->
                    group(sourceSets = setOf(platform), styles = setOf(ContentStyle.KDocTag)) {
                        with(provider) {
                            contentForDescription(platform, tag)
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
    sourceSets.forEach { platform ->
        unnamedTags[platform]?.let { tags ->
            if (tags.isNotEmpty()) {
                tags.groupBy { it::class }.forEach { (_, sameCategoryTags) ->
                    group(sourceSets = setOf(platform), styles = setOf(ContentStyle.KDocTag)) {
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
    availableSourceSets.forEach { platform ->
        header(KDOC_TAG_HEADER_LEVEL, "Parameters", kind = ContentKind.Parameters, sourceSets = setOf(platform))
        table(
            kind = ContentKind.Parameters,
            extra = mainExtra + SimpleAttr.header("Parameters"),
            sourceSets = setOf(platform)
        )
        {
            val possibleFallbacks = availableSourceSets.getPossibleFallback(platform)
            params.mapNotNull { (_, param) ->
                (param[platform] ?: param.fallback(possibleFallbacks))?.let {
                    row(sourceSets = setOf(platform), kind = ContentKind.Parameters) {
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

    val availablePlatforms = seeAlsoTags.availableSourceSets()
    availablePlatforms.forEach { platform ->
        header(KDOC_TAG_HEADER_LEVEL, "See also", kind = ContentKind.Comment, sourceSets = setOf(platform))

        table(
            kind = ContentKind.Comment,
            extra = mainExtra + SimpleAttr.header("See also")
        )
        {
            val possibleFallbacks = availablePlatforms.getPossibleFallback(platform)
            seeAlsoTags.forEach { (_, see) ->
                (see[platform] ?: see.fallback(possibleFallbacks))?.let { seeTag ->
                    row(
                        sourceSets = setOf(platform),
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
    val throws = tags.withTypeNamed<Throws>() ?: return

    throws.availableSourceSets().forEach { platform ->
        header(KDOC_TAG_HEADER_LEVEL, "Throws", sourceSets = setOf(platform))
        table(
            kind = ContentKind.Main,
            sourceSets = setOf(platform),
            extra = mainExtra + SimpleAttr.header("Throws")
        ) {
            throws.entries.forEach { entry ->
                entry.value[platform]?.let { throws ->
                    row(sourceSets = setOf(platform)) {
                        group(styles = mainStyles + ContentStyle.RowTitle) {
                            throws.exceptionAddress?.let {
                                val className = it.takeIf { it.target is PointingToDeclaration }?.classNames
                                link(text = className ?: entry.key, address = it)
                            } ?: text(entry.key)
                        }
                        if (throws.isNotEmpty()) {
                            comment(throws.root)
                        }
                    }
                }
            }
        }
    }
}

internal fun PageContentBuilder.DocumentableContentBuilder.samplesSectionContent(tags: GroupedTags) {
    val samples = tags.withTypeNamed<Sample>() ?: return
    samples.availableSourceSets().forEach { platform ->
        val content = samples.filter { it.value.isEmpty() || platform in it.value }
        header(KDOC_TAG_HEADER_LEVEL, "Samples", kind = ContentKind.Sample, sourceSets = setOf(platform))

        group(
            sourceSets = setOf(platform),
            kind = ContentKind.Sample,
            styles = setOf(TextStyle.Monospace, ContentStyle.RunnableSample),
            extra = mainExtra + SimpleAttr.header("Samples")
        ) {
            content.forEach {
                text(it.key)
            }
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

    availableSourceSets.forEach { platform ->
        header(KDOC_TAG_HEADER_LEVEL, "Inheritors", sourceSets = setOf(platform))
        table(
            kind = ContentKind.Inheritors,
            sourceSets = setOf(platform),
            extra = mainExtra + SimpleAttr.header("Inheritors")
        ) {
            inheritors[platform]?.forEach { classlike: DRI ->
                inheritorRow(classlike, logger)
            }
        }
    }
}

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

private fun PageContentBuilder.TableBuilder.inheritorRow(classlike: DRI, logger: DokkaLogger) = row {
    link(
        text = classlike.friendlyClassName()
            ?: classlike.toString().also { logger.warn("No class name found for DRI $classlike") },
        address = classlike
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


