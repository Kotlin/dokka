package org.jetbrains.dokka.xml

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.transformers.descriptors.XMLMega
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer

class XmlPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer providing ::XmlTransformer
    }
}

class XmlTransformer(private val dokkaContext: DokkaContext) : PageNodeTransformer {
    private val commentsToContentConverter by lazy { dokkaContext.single(CoreExtensions.commentsToContentConverter) }

    enum class XMLKind : Kind {
        Main, XmlList
    }

    override fun invoke(input: RootPageNode): RootPageNode =
        input.transformPageNodeTree { if (it is ModulePageNode) transformModule(it) else it }

    private fun transformModule(module: ModulePageNode) = module.transformContentPagesTree { node ->
        if (node !is ClassPageNode) node
        else {
            val refs =
                node.documentable?.extra?.filterIsInstance<XMLMega>()?.filter { it.key == "@attr ref" }
                    .orEmpty()
            val elementsToAdd = mutableListOf<Documentable>()

            refs.forEach { ref ->
                module.documentable?.dfs { it.dri == ref.dri }?.let { elementsToAdd.add(it) }
            }
            val platformData = node.platforms().toSet()
            val refTable = DefaultPageContentBuilder.group(
                node.dri,
                platformData,
                XMLKind.XmlList,
                commentsToContentConverter,
                dokkaContext.logger
            ) {
                block("XML Attributes", 2, XMLKind.XmlList, elementsToAdd, platformData) { element ->
                    link(element.dri, XMLKind.XmlList) {
                        text(element.name ?: "<unnamed>", XMLKind.Main)
                    }
                    text(element.briefDocTagString, XMLKind.XmlList)
                }
            }

            val content = node.content as ContentGroup
            val children = (node.content as ContentGroup).children
            node.modified(content = content.copy(children = children + refTable))
        }
    }

    private fun ContentPage.platforms() = this.content.platforms.toList()
}