package org.jetbrains.dokka.webhelp.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.DefaultPageCreator
import org.jetbrains.dokka.base.translators.documentables.DocumentableContentBuilderFactory
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

open class WebhelpPageCreator(
    configuration: DokkaBaseConfiguration?,
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    logger: DokkaLogger
) : DefaultPageCreator(configuration, commentsToContentConverter, signatureProvider, logger) {
    override val contentBuilder: PageContentBuilder =
        WebhelpPageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    override fun contentForScope(
        s: WithScope,
        dri: DRI,
        sourceSets: Set<DokkaConfiguration.DokkaSourceSet>
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
                header(2, "Inheritors")
                +ContentList(
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
                    extra = mainExtra + SimpleAttr.header("Inheritors"),
                    ordered = true
                )
            }
        }
    }
}

open class WebhelpPageContentBuilder(
    commentsConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    logger: DokkaLogger
) : PageContentBuilder(commentsConverter, signatureProvider, logger) {
    override val documentableContentBuilderFactory =
        DocumentableContentBuilderFactory { dris, sourcesets, styles, extras ->
            CoverlessContentBuilder(dris, sourcesets, styles, extras)
        }

    open inner class CoverlessContentBuilder(
        mainDRI: Set<DRI>,
        mainSourcesetData: Set<DokkaConfiguration.DokkaSourceSet>,
        mainStyles: Set<Style>,
        mainExtra: PropertyContainer<ContentNode>
    ) : DocumentableContentBuilder(mainDRI, mainSourcesetData, mainStyles, mainExtra) {
        override fun cover(
            text: String,
            sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
            styles: Set<Style>,
            extra: PropertyContainer<ContentNode>,
            block: PageContentBuilder.DocumentableContentBuilder.() -> Unit
        ) {
        }
    }
}