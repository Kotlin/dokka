package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.pages.PageTransformer
import kotlin.collections.HashMap

val preprocessors = listOf(ResourcesInstaller, TreeViewInstaller, AllClassesPageInstaller, IndexGenerator)

object ResourcesInstaller : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.modified(
        children = input.children +
                RendererSpecificResourcePage(
                    "resourcePack",
                    emptyList(),
                    RenderingStrategy.Copy("/static_res")
                )
    )
}

object TreeViewInstaller : PageTransformer {
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
            documentable = node.documentable,
            root = root
        )

        return node.modified(children = node.children.map { node ->
            install(
                node,
                root
            )
        } + overviewTree) as JavadocModulePageNode
    }

    private fun installPackageTreeNode(node: JavadocPackagePageNode, root: RootPageNode): JavadocPackagePageNode {
        val packageTree = TreeViewPage(
            name = "${node.name}",
            packages = null,
            classes = node.children.filterIsInstance<JavadocClasslikePageNode>(),
            dri = node.dri,
            documentable = node.documentable,
            root = root
        )

        return node.modified(children = node.children + packageTree) as JavadocPackagePageNode
    }
}

object AllClassesPageInstaller : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        val classes = (input as JavadocModulePageNode).children.filterIsInstance<JavadocPackagePageNode>().flatMap {
            it.children.filterIsInstance<JavadocClasslikePageNode>()
        }

        return input.modified(children = input.children + AllClassesPage(classes))
    }
}

object IndexGenerator: PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        val elements = HashMap<Char, MutableSet<IndexableJavadocNode>>()
        (input as JavadocModulePageNode).children.filterIsInstance<JavadocPackagePageNode>().forEach {
            it.getAllIndexables().forEach { d ->
                val name = when(d) {
                    is JavadocPageNode -> d.name
                    is AnchorableJavadocNode -> d.name
                    else -> null
                }
                if (name != null && name.isNotBlank()) {
                    elements.getOrPut(name[0].toUpperCase(), ::mutableSetOf).add(d)
                }
            }
        }
        val keys = elements.keys.sortedBy { it }
        return input.modified(children = input.children + elements.entries.mapIndexed { i, (_, set) ->
            IndexPage(i + 1, set.sortedBy { it.getId() }, keys, input.sourceSets())
        })
    }
}