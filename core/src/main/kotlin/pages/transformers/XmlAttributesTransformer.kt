package org.jetbrains.dokka.pages.transformers

import org.jetbrains.dokka.DefaultExtra
import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.Model.dfs
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.renderers.platforms

object XmlAttributesTransformer : PageNodeTransformer {
    enum class XMLKind : Kind {
        Main, XmlList
    }

    override fun invoke(original: ModulePageNode): ModulePageNode {
        original.findAll { it is ClassPageNode }.forEach { node ->
            val refs = node.documentationNode?.extra?.filterIsInstance<DefaultExtra>()?.filter { it.key == "@attr ref"}.orEmpty()
            val elementsToAdd = mutableListOf<DocumentationNode<*>>()

            refs.forEach { ref ->
                val toFind = DRI.from(ref.value)
                original.documentationNode?.dfs { it.dri == toFind }?.let { elementsToAdd.add(it) }
            }

            val refTable = ContentGroup(
                listOf(
                    ContentHeader(
                        listOf(
                            ContentText(
                                "XML Attributes",
                                DCI(node.dri, XMLKind.XmlList),
                                node.platforms().toSet(), emptySet(), emptySet()
                            )
                        ),
                        2,
                        DCI(node.dri, XMLKind.Main),
                        node.platforms().toSet(), emptySet(), emptySet()
                    ),
                    ContentTable(
                        emptyList(),
                        elementsToAdd.map {
                            ContentGroup(
                                listOf(
                                    ContentDRILink(
                                        listOf(
                                            ContentText(
                                                it.descriptors.first().name.toString(),
                                                DCI(node.dri, XMLKind.Main),
                                                node.platforms().toSet(), emptySet(), emptySet()
                                            )
                                        ),
                                        it.dri,
                                        DCI(it.dri, XMLKind.XmlList),
                                        it.platformData.toSet(), emptySet(), emptySet()
                                    ),
                                    ContentText(
                                        it.briefDocstring,
                                        DCI(it.dri, XMLKind.XmlList),
                                        it.platformData.toSet(), emptySet(), emptySet()
                                    )
                                ),
                                DCI(node.dri, XMLKind.XmlList),
                                node.platforms().toSet(), emptySet(), emptySet()
                            )
                        },
                        DCI(node.dri, XMLKind.XmlList),
                        node.platforms().toSet(), emptySet(), emptySet()
                    )
                ),
                DCI(node.dri, XMLKind.XmlList),
                node.platforms().toSet(), emptySet(), emptySet()
            )

            val content = node.content as ContentGroup
            val children = (node.content as ContentGroup).children
            node.content = content.copy(children = children + refTable)
        }
        return original
    }
}
