package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
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
    platforms: Set<DokkaConfiguration.DokkaSourceSet>
) {
    val descriptions = documentable.descriptions
    if (descriptions.any { it.value.root.children.isNotEmpty() }) {
        platforms.forEach { platform ->
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
    platforms: Set<DokkaConfiguration.DokkaSourceSet>,
    customTagContentProviders: List<CustomTagContentProvider>
) {
    val customTags = documentable.customTags ?: return

    platforms.forEach { platform ->
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
    platforms: Set<DokkaConfiguration.DokkaSourceSet>,
    toHeaderString: TagWrapper.() -> String
) {
    val unnamedTags = documentable.groupedTags
        .filterNot { (k, _) -> k.isSubclassOf(NamedTagWrapper::class) || k in specialTags }
        .values.flatten().groupBy { it.first }.mapValues { it.value.map { it.second } }
    if (unnamedTags.isEmpty()) return
    platforms.forEach { platform ->
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

    val availablePlatforms = params.availablePlatforms()
    availablePlatforms.forEach { platform ->
        header(KDOC_TAG_HEADER_LEVEL, "Parameters", kind = ContentKind.Parameters, sourceSets = setOf(platform))
        table(
            kind = ContentKind.Parameters,
            extra = mainExtra + SimpleAttr.header("Parameters"),
            sourceSets = setOf(platform)
        )
        {
            val possibleFallbacks = availablePlatforms.getPossibleFallbackSourcesets(platform)
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

    val availablePlatforms = seeAlsoTags.availablePlatforms()
    availablePlatforms.forEach { platform ->
        header(KDOC_TAG_HEADER_LEVEL, "See also", kind = ContentKind.Comment, sourceSets = setOf(platform))

        table(
            kind = ContentKind.Comment,
            extra = mainExtra + SimpleAttr.header("See also")
        )
        {
            val possibleFallbacks = availablePlatforms.getPossibleFallbackSourcesets(platform)
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

internal fun PageContentBuilder.DocumentableContentBuilder.throwsSectionContent(
    tags: GroupedTags
) {
    val throws = tags.withTypeNamed<Throws>() ?: return

    val availablePlatforms = throws.values.flatMap { it.keys }.toSet()
    availablePlatforms.forEach { platform ->
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

    samples.availablePlatforms().forEach { platformData ->
        val content = samples.filter { it.value.isEmpty() || platformData in it.value }
        header(KDOC_TAG_HEADER_LEVEL, "Samples", kind = ContentKind.Sample, sourceSets = setOf(platformData))
        group(
            sourceSets = setOf(platformData),
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
    documentables: List<Documentable>,
    logger: DokkaLogger
) {
    val inheritors = documentables.filterIsInstance<WithScope>().inheritors()
    if (inheritors.values.none()) return

    val availablePlatforms = inheritors.keys.toSet()
    availablePlatforms.forEach { platform ->
        header(KDOC_TAG_HEADER_LEVEL, "Inheritors", sourceSets = setOf(platform))
        table(
            kind = ContentKind.Inheritors,
            sourceSets = setOf(platform),
            extra = mainExtra + SimpleAttr.header("Inheritors")
        ) {
            inheritors[platform]?.forEach { classlike ->
                row {
                    link(
                        text = classlike.friendlyClassName()
                            ?: classlike.toString().also { logger.warn("No class name found for DRI $classlike") },
                        address = classlike
                    )
                }
            }
        }
    }
}

private fun List<WithScope>.inheritors() =
    fold(mutableMapOf<DokkaConfiguration.DokkaSourceSet, List<DRI>>()) { acc, scope ->
        val inheritorsForScope =
            scope.safeAs<WithExtraProperties<Documentable>>()?.let { it.extra[InheritorsInfo] }?.let { inheritors ->
                inheritors.value.filter { it.value.isNotEmpty() }
            }.orEmpty()
        inheritorsForScope.forEach { (k, v) ->
            acc.compute(k) { _, value -> value?.plus(v) ?: v }
        }
        acc
    }

private fun DRI.friendlyClassName() = classNames?.substringAfterLast(".")

private fun TagWrapper.isNotEmpty() = this.children.isNotEmpty()

private fun <V> Map<DokkaConfiguration.DokkaSourceSet, V>.fallback(sourceSets: List<DokkaConfiguration.DokkaSourceSet>): V? =
    sourceSets.firstOrNull { it in this.keys }.let { this[it] }

/**
 * Used for multi-value tags (e.g. params) when values are missed on some platforms.
 * It this case description is inherited from parent platform.
 * E.g. if param hasn't description in JVM, the description is taken from common.
 */
private fun Set<DokkaConfiguration.DokkaSourceSet>.getPossibleFallbackSourcesets(sourceSet: DokkaConfiguration.DokkaSourceSet) =
    this.filter { it.sourceSetID in sourceSet.dependentSourceSets }

private val specialTags: Set<KClass<out TagWrapper>> =
    setOf(Property::class, Description::class, Constructor::class, Param::class, See::class)

private fun <T> Map<String, SourceSetDependent<T>>.availablePlatforms() = values.flatMap { it.keys }.toSet()


