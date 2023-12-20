/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.transformers.documentables.isDeprecated
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

/**
 * Merges [MemberPage] elements that have the same name.
 * That includes **both** properties and functions.
 */
public class SameMethodNamePageMergerStrategy(
    public val logger: DokkaLogger
) : PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>, path: List<String>): List<PageNode> {
        val members = pages
            .filterIsInstance<MemberPageNode>()
            .takeIf { it.isNotEmpty() }
            ?.sortedBy { it.containsDeprecatedDocumentables() } // non-deprecated first
            ?: return pages

        val name = pages.first().name.also {
            if (pages.any { page -> page.name != it }) { // Is this even possible?
                logger.error("Page names for $it do not match!")
            }
        }
        val dri = members.flatMap { it.dri }.toSet()


        val merged = MemberPageNode(
            dri = dri,
            name = name,
            children = members.flatMap { it.children }.distinct(),
            content = squashDivergentInstances(members).withSourceSets(members.allSourceSets()),
            embeddedResources = members.flatMap { it.embeddedResources }.distinct(),
            documentables = members.flatMap { it.documentables }
        )

        return (pages - members) + listOf(merged)
    }

    @Suppress("UNCHECKED_CAST")
    private fun MemberPageNode.containsDeprecatedDocumentables() =
        this.documentables.any { (it as? WithExtraProperties<Documentable>)?.isDeprecated() == true }

    private fun List<MemberPageNode>.allSourceSets(): Set<DisplaySourceSet> =
        fold(emptySet()) { acc, e -> acc + e.sourceSets() }

    private fun squashDivergentInstances(nodes: List<MemberPageNode>): ContentNode =
        nodes.map { it.content }
            .reduce { acc, node ->
                acc.mapTransform<ContentDivergentGroup, ContentNode> { g ->
                    g.copy(children = (g.children +
                            ((node.dfs { it is ContentDivergentGroup && it.groupID == g.groupID } as? ContentDivergentGroup)
                                ?.children ?: emptyList())
                            )
                    )
                }
            }
}
