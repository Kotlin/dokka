package org.jetbrains.dokka.xml

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DefaultExtra
import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.Model.dfs
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.PageNodeTransformer

class XmlPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer with XmlTransformer
    }
}

object XmlTransformer : PageNodeTransformer {
    enum class XMLKind : Kind {
        Main, XmlList
    }

    override fun invoke(input: ModulePageNode, dokkaContext: DokkaContext): ModulePageNode {
        input.findAll { it is ClassPageNode }.forEach { node ->
            val refs = node.documentationNode?.extra?.filterIsInstance<DefaultExtra>()?.filter { it.key == "@attr ref"}.orEmpty()
            val elementsToAdd = mutableListOf<DocumentationNode<*>>()

            refs.forEach { ref ->
                val toFind = DRI.from(ref.value)
                input.documentationNode?.dfs { it.dri == toFind }?.let { elementsToAdd.add(it) }
            }
            val refTable = group(node.platforms().toSet(), node.dri, XMLKind.XmlList) {
                table("XML Attributes", 2, XMLKind.XmlList, elementsToAdd) {
                    elementsToAdd.map { row(it) }
                }
            }

            val content = node.content as ContentGroup
            val children = (node.content as ContentGroup).children
            node.content = content.copy(children = children + refTable)
        }
        return input
    }

    private fun PageNode.findAll(predicate: (PageNode) -> Boolean): Set<PageNode> {
        val found = mutableSetOf<PageNode>()
        if (predicate(this)) {
            found.add(this)
        } else {
            this.children.asSequence().mapNotNull { it.findAll(predicate) }.forEach { found.addAll(it) }
        }
        return found
    }

    private fun PageNode.platforms() = this.content.platforms.toList()
}