/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.transformers.documentables.deprecatedAnnotation
import org.jetbrains.dokka.base.transformers.documentables.isDeprecated
import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.BooleanValue
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin

public object ResourcesInstaller : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode = input.modified(
        children = input.children +
                RendererSpecificResourcePage(
                    "resourcePack",
                    emptyList(),
                    RenderingStrategy.Copy("/static_res")
                )
    )
}

public class TreeViewInstaller(
    private val context: DokkaContext
) : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode = install(input, input) as RootPageNode

    private fun install(node: PageNode, root: RootPageNode): PageNode = when (node) {
        is JavadocModulePageNode -> installOverviewTreeNode(node, root)
        is JavadocPackagePageNode -> installPackageTreeNode(node, root)
        else -> node
    }

    private fun installOverviewTreeNode(node: JavadocModulePageNode, root: RootPageNode): JavadocModulePageNode {
        val overviewTree = TreeViewPage(
            name = "Class Hierarchy",
            packages = node.children<JavadocPackagePageNode>().map { installPackageTreeNode(it, root) },
            classes = null,
            dri = node.dri,
            documentables = node.documentables,
            root = root,
            inheritanceBuilder = context.plugin<InternalKotlinAnalysisPlugin>().querySingle { inheritanceBuilder }
        )

        val nodeChildren = node.children.map { childNode ->
            install(
                childNode,
                root
            )
        }
        return node.modified(children = nodeChildren + overviewTree) as JavadocModulePageNode
    }

    private fun installPackageTreeNode(node: JavadocPackagePageNode, root: RootPageNode): JavadocPackagePageNode {
        val packageTree = TreeViewPage(
            name = node.name,
            packages = null,
            classes = node.children.filterIsInstance<JavadocClasslikePageNode>(),
            dri = node.dri,
            documentables = node.documentables,
            root = root,
            inheritanceBuilder = context.plugin<InternalKotlinAnalysisPlugin>().querySingle { inheritanceBuilder }
        )

        return node.modified(children = node.children + packageTree) as JavadocPackagePageNode
    }
}

public object AllClassesPageInstaller : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode {
        val classes = (input as JavadocModulePageNode).children.filterIsInstance<JavadocPackagePageNode>().flatMap {
            it.children.filterIsInstance<JavadocClasslikePageNode>()
        }

        return input.modified(children = input.children + AllClassesPage(classes))
    }
}

public object IndexGenerator : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode {
        val elements = HashMap<Char, MutableSet<NavigableJavadocNode>>()
        (input as JavadocModulePageNode).children.filterIsInstance<JavadocPackagePageNode>().forEach {
            it.getAllNavigables().forEach { d ->
                val name = when (d) {
                    is JavadocPageNode -> d.name
                    is AnchorableJavadocNode -> d.name
                    else -> null
                }
                if (name != null && name.isNotBlank()) {
                    elements.getOrPut(name[0].toUpperCase(), ::mutableSetOf).add(d)
                }
            }
            elements.getOrPut(it.name[0].toUpperCase(), ::mutableSetOf).add(it)
        }
        val keys = elements.keys.sortedBy { it }
        val sortedElements = elements.entries.sortedBy { (a, _) -> a }

        val indexNodeComparator = getIndexNodeComparator()
        val indexPages = sortedElements.mapIndexed { idx, (_, set) ->
            IndexPage(
                id = idx + 1,
                elements = set.sortedWith(indexNodeComparator),
                keys = keys,
                sourceSet = input.sourceSets()
            )
        }
        return input.modified(children = input.children + indexPages)
    }

    private fun getIndexNodeComparator(): Comparator<NavigableJavadocNode> {
        val driComparator = compareBy<DRI> { it.packageName }
            .thenBy { it.classNames }
            .thenBy { it.callable?.name.orEmpty() }
            .thenBy { it.callable?.signature().orEmpty() }

        return compareBy<NavigableJavadocNode> { it.getId().toLowerCase() }
            .thenBy(driComparator) { it.getDRI() }
    }
}

public object DeprecatedPageCreator : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode {
        val elements = HashMap<DeprecatedPageSection, MutableSet<DeprecatedNode>>().apply {

            fun <T, V> T.putAs(deprecatedPageSection: DeprecatedPageSection) where
                    T : NavigableJavadocNode,
                    V : Documentable,
                    T : WithJavadocExtra<V> {
                val deprecatedNode = DeprecatedNode(
                    listOfNotNull(
                        getDRI().packageName?.takeUnless { it.isBlank() },
                        if (this is JavadocFunctionNode) getAnchor() else getId()
                    ).joinToString("."),
                    getDRI(),
                    (this as? WithBrief)?.brief.orEmpty()
                )
                getOrPut(deprecatedPageSection) { mutableSetOf() }.add(deprecatedNode)
                if (deprecatedAnnotation?.params?.get("forRemoval") == BooleanValue(true)) {
                    getOrPut(DeprecatedPageSection.DeprecatedForRemoval) { mutableSetOf() }.add(deprecatedNode)
                }
            }

            fun verifyDeprecation(node: NavigableJavadocNode) {
                when (node) {
                    is JavadocModulePageNode -> {
                        node.children.filterIsInstance<JavadocPackagePageNode>().forEach(::verifyDeprecation)
                        node.takeIf { it.isDeprecated() }?.putAs(DeprecatedPageSection.DeprecatedModules)
                    }
                    is JavadocPackagePageNode ->
                        node.children.filterIsInstance<NavigableJavadocNode>().forEach(::verifyDeprecation)
                    is JavadocClasslikePageNode -> {
                        node.classlikes.forEach(::verifyDeprecation)
                        node.methods.forEach {
                            it.takeIf { it.isDeprecated() }?.putAs(DeprecatedPageSection.DeprecatedMethods)
                        }
                        node.constructors.forEach {
                            it.takeIf { it.isDeprecated() }?.putAs(DeprecatedPageSection.DeprecatedConstructors)
                        }
                        node.properties.forEach {
                            it.takeIf { it.isDeprecated() }?.putAs(DeprecatedPageSection.DeprecatedFields)
                        }
                        node.entries.forEach {
                            it.takeIf { it.isDeprecated() }?.putAs(DeprecatedPageSection.DeprecatedEnumConstants)
                        }
                        node.takeIf { it.isDeprecated() }?.putAs(
                            if ((node as? WithJavadocExtra<*>)?.isException == true) DeprecatedPageSection.DeprecatedExceptions
                            else when (node.kind) {
                                "enum" -> DeprecatedPageSection.DeprecatedEnums
                                "interface" -> DeprecatedPageSection.DeprecatedInterfaces
                                else -> DeprecatedPageSection.DeprecatedClasses
                            }
                        )
                    }
                }
            }

            verifyDeprecation(input as JavadocModulePageNode)
        }
        return input.modified(
            children = input.children + DeprecatedPage(
                elements,
                (input as JavadocModulePageNode).sourceSets()
            )
        )
    }
}
