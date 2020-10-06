package org.jetbrains.dokka.javadoc.renderer

import org.jetbrains.dokka.javadoc.location.JavadocLocationProvider
import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.javadoc.toNormalized
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.ImplementedInterfaces
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.formatToEndWithHtml
import java.io.File
import java.nio.file.Paths

internal class JavadocContentToTemplateMapTranslator(
    private val locationProvider: JavadocLocationProvider,
    private val context: DokkaContext,
) {

    fun templateMapForPageNode(node: JavadocPageNode): TemplateMap =
        mapOf(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot(node),
            "contextRoot" to node,
            "kind" to "main",
        ) + templateMapForNode(node)


    private fun templateMapForNode(node: JavadocPageNode): TemplateMap =
        when (node) {
            is JavadocModulePageNode -> InnerTranslator(node).templateMapForJavadocContentNode(node.content)
            is JavadocClasslikePageNode -> InnerTranslator(node).templateMapForClasslikeNode(node)
            is JavadocPackagePageNode -> InnerTranslator(node).templateMapForPackagePageNode(node)
            is TreeViewPage -> InnerTranslator(node).templateMapForTreeViewPage(node)
            is AllClassesPage -> InnerTranslator(node).templateMapForAllClassesPage(node)
            is IndexPage -> InnerTranslator(node).templateMapForIndexPage(node)
            is DeprecatedPage -> InnerTranslator(node).templateMapForDeprecatedPage(node)
            else -> emptyMap()
        }

    private fun pathToRoot(node: JavadocPageNode): String {
        return when (node) {
            is JavadocModulePageNode -> ""
            else -> run {
                val link = locationProvider.resolve(node, skipExtension = true)
                val dir = Paths.get(link).parent?.toNormalized().orEmpty()
                return dir.split(File.separator).filter { it.isNotEmpty() }.joinToString("/") { ".." }.let {
                    if (it.isNotEmpty()) "$it/" else it
                }
            }
        }
    }

    private inner class InnerTranslator(val contextNode: JavadocPageNode) {

        private val htmlTranslator = JavadocContentToHtmlTranslator(locationProvider, context)

        fun templateMapForAllClassesPage(node: AllClassesPage): TemplateMap =
            mapOf(
                "title" to "All Classes",
                "list" to node.classEntries
            )

        fun templateMapForIndexPage(node: IndexPage): TemplateMap =
            mapOf(
                "id" to node.id,
                "title" to node.title,
                "kind" to "indexPage",
                "prevLetter" to if (node.id > 1) "index-${node.id - 1}" else "",
                "nextLetter" to if (node.id < node.keys.size) "index-${node.id + 1}" else "",
                "dictionary" to node.keys,
                "elements" to node.elements.map { templateMapForIndexableNode(it) }
            )

        fun templateMapForDeprecatedPage(node: DeprecatedPage): TemplateMap =
            mapOf(
                "id" to node.name,
                "title" to "Deprecated",
                "kind" to "deprecated",
                "sections" to node.elements.toList().sortedBy { (section, _) -> section.priority }
                    .map { (s, e) -> templateMapForDeprecatedPageSection(s, e) }
            )

        fun templateMapForDeprecatedPageSection(
            section: DeprecatedPageSection,
            elements: Set<DeprecatedNode>
        ): TemplateMap =
            mapOf(
                "id" to section.id,
                "header" to section.header,
                "caption" to section.caption,
                "elements" to elements.map { node ->
                    mapOf(
                        "name" to node.name,
                        "address" to locationProvider.resolve(node.address, contextNode.sourceSets(), contextNode)
                            ?.formatToEndWithHtml().orEmpty(),
                        "description" to htmlForContentNodes(node.description, contextNode)
                    )
                }
            )

        fun templateMapForTreeViewPage(node: TreeViewPage): TemplateMap =
            mapOf(
                "title" to node.title,
                "name" to node.name,
                "kind" to node.kind,
                "list" to node.packages.orEmpty() + node.classes.orEmpty(),
                "classGraph" to node.classGraph,
                "interfaceGraph" to node.interfaceGraph
            )

        fun templateMapForPackagePageNode(node: JavadocPackagePageNode): TemplateMap = mapOf(
            "kind" to "package"
        ) + templateMapForJavadocContentNode(node.content)

        fun templateMapForFunctionNode(node: JavadocFunctionNode): TemplateMap = mapOf(
            "brief" to htmlForContentNodes(node.brief, contextNode),
            "description" to htmlForContentNodes(node.description,contextNode),
            "parameters" to node.parameters.map { templateMapForParameterNode(it) },
            "inlineParameters" to node.parameters.joinToString { renderInlineParameter(it) },
            "anchorLink" to node.getAnchor(),
            "signature" to templateMapForSignatureNode(node.signature),
            "name" to node.name
        )

        fun templateMapForClasslikeNode(node: JavadocClasslikePageNode): TemplateMap =
            mapOf(
                "constructors" to node.constructors.map { templateMapForFunctionNode(it) },
                "signature" to templateMapForSignatureNode(node.signature),
                "methods" to templateMapForClasslikeMethods(node.methods),
                "classlikeDocumentation" to htmlForContentNodes(node.description, node),
                "entries" to node.entries.map { templateMapForEntryNode(it) },
                "properties" to node.properties.map { templateMapForPropertyNode(it) },
                "classlikes" to node.classlikes.map { templateMapForNestedClasslikeNode(it) },
                "implementedInterfaces" to templateMapForImplementedInterfaces(node).sorted(),
                "kind" to node.kind,
                "packageName" to node.packageName,
                "name" to node.name
            ) + templateMapForJavadocContentNode(node.content)

        fun templateMapForSignatureNode(node: JavadocSignatureContentNode): TemplateMap =
            mapOf(
                "annotations" to node.annotations?.let { htmlForContentNode(it, contextNode) },
                "signatureWithoutModifiers" to htmlForContentNode(node.signatureWithoutModifiers, contextNode),
                "modifiers" to node.modifiers?.let { htmlForContentNode(it, contextNode) },
                "supertypes" to node.supertypes?.let { htmlForContentNode(it, contextNode) }
            )

        private fun NavigableJavadocNode.typeForIndexable() = when (this) {
            is JavadocClasslikePageNode -> "class"
            is JavadocFunctionNode -> "function"
            is JavadocEntryNode -> "enum entry"
            is JavadocParameterNode -> "parameter"
            is JavadocPropertyNode -> "property"
            is JavadocPackagePageNode -> "package"
            else -> ""
        }

        fun templateMapForIndexableNode(node: NavigableJavadocNode): TemplateMap {
            val origin = node.getDRI().parent
            return mapOf(
                "address" to locationProvider.resolve(node.getDRI(), contextNode.sourceSets(), contextNode)
                    ?.formatToEndWithHtml().orEmpty(),
                "type" to node.typeForIndexable(),
                "isMember" to (node !is JavadocPackagePageNode),
                "name" to if (node is JavadocFunctionNode) node.getAnchor() else node.getId(),
                "description" to ((node as? WithBrief)?.let {
                    htmlForContentNodes(
                        it.brief,
                        contextNode
                    ).takeIf { desc -> desc.isNotBlank() }
                } ?: "&nbsp;"),
                "origin" to origin.indexableOriginSignature(),
            )
        }

        private fun DRI.indexableOriginSignature(): String {
            val packageName = packageName?.takeIf { it.isNotBlank() }
            val className = classNames?.let {
                "<a href=${locationProvider.resolve(this, contextNode.sourceSets(), contextNode)
                    ?.formatToEndWithHtml().orEmpty()}>$it</a>"
            }
            return listOfNotNull(packageName, className).joinToString(".")
        }

        fun templateMapForJavadocContentNode(node: JavadocContentNode): TemplateMap =
            when (node) {
                is TitleNode -> templateMapForTitleNode(node)
                is JavadocContentGroup -> templateMapForJavadocContentGroup(node)
                is LeafListNode -> templateMapForLeafListNode(node)
                is RootListNode -> templateMapForRootListNode(node)
                else -> emptyMap()
            }

        fun templateMapForJavadocContentNode(node: ContentNode): TemplateMap = (node as? JavadocContentNode)?.let { templateMapForJavadocContentNode(it) } ?: emptyMap()

        private fun templateMapForParameterNode(node: JavadocParameterNode): TemplateMap =
            mapOf(
                "description" to htmlForContentNodes(node.description, contextNode),
                "name" to node.name,
                "type" to htmlForContentNode(node.type, contextNode)
            )

        private fun templateMapForImplementedInterfaces(node: JavadocClasslikePageNode) =
            node.extra[ImplementedInterfaces]?.interfaces?.entries?.firstOrNull { it.key.analysisPlatform == Platform.jvm }?.value?.map { it.dri.displayable() } // TODO: REMOVE HARDCODED JVM DEPENDENCY
                .orEmpty()

        private fun templateMapForClasslikeMethods(nodes: List<JavadocFunctionNode>): TemplateMap {
            val (inherited, own) = nodes.partition { it.isInherited }
            return mapOf(
                "own" to own.map { templateMapForFunctionNode(it) },
                "inherited" to inherited.map { templateMapForInheritedMethod(it) }
                    .groupBy { it["inheritedFrom"] as String }.entries.map {
                        mapOf(
                            "inheritedFrom" to it.key,
                            "names" to it.value.map { it["name"] as String }.sorted().joinToString()
                        )
                    }
            )
        }

        private fun templateMapForInheritedMethod(node: JavadocFunctionNode): TemplateMap {
            val inheritedFrom = node.extra[InheritedMember]?.inheritedFrom
            return mapOf(
                "inheritedFrom" to inheritedFrom?.entries?.firstOrNull { it.key.analysisPlatform == Platform.jvm }?.value?.displayable() // TODO: REMOVE HARDCODED JVM DEPENDENCY
                    .orEmpty(),
                "name" to node.name
            )
        }

        private fun templateMapForNestedClasslikeNode(node: JavadocClasslikePageNode): TemplateMap {
            return mapOf(
                "modifiers" to node.signature.modifiers?.let { htmlForContentNode(it, contextNode) },
                "signature" to node.name,
                "address" to locationProvider.resolve(
                    contextNode.children.first { (it as? JavadocClasslikePageNode)?.dri?.first() == node.dri.first() },
                    contextNode
                ).formatToEndWithHtml(),
                "description" to htmlForContentNodes(node.description, node)
            )
        }

        private fun templateMapForPropertyNode(node: JavadocPropertyNode): TemplateMap {
            return mapOf(
                "modifiers" to node.signature.modifiers?.let { htmlForContentNode(it, contextNode) },
                "signature" to htmlForContentNode(node.signature.signatureWithoutModifiers, contextNode),
                "description" to htmlForContentNodes(node.brief, contextNode)
            )
        }

        private fun templateMapForEntryNode(node: JavadocEntryNode): TemplateMap {
            return mapOf(
                "signature" to templateMapForSignatureNode(node.signature),
                "brief" to htmlForContentNodes(node.brief, contextNode)
            )
        }

        private fun templateMapForTitleNode(node: TitleNode): TemplateMap {
            return mapOf(
                "title" to node.title,
                "subtitle" to htmlForContentNodes(node.subtitle, contextNode),
                "version" to node.version,
                "packageName" to node.parent
            )
        }

        private fun templateMapForJavadocContentGroup(note: JavadocContentGroup): TemplateMap {
            return note.children.fold(emptyMap()) { map, child ->
                map + templateMapForJavadocContentNode(child)
            }
        }

        private fun templateMapForLeafListNode(node: LeafListNode): TemplateMap {
            return mapOf(
                "tabTitle" to node.tabTitle,
                "colTitle" to node.colTitle,
                "list" to node.entries
            )
        }

        private fun templateMapForRootListNode(node: RootListNode): TemplateMap {
            return mapOf(
                "lists" to node.entries.map { templateMapForLeafListNode(it) }
            )
        }

        private fun renderInlineParameter(parameter: JavadocParameterNode): String =
            htmlForContentNode(parameter.type, contextNode) + " ${parameter.name}"

        private fun htmlForContentNode(node: ContentNode, relativeNode: PageNode) =
            htmlTranslator.htmlForContentNode(node, relativeNode)

        private fun htmlForContentNodes(nodes: List<ContentNode>, relativeNode: PageNode) =
            htmlTranslator.htmlForContentNodes(nodes, relativeNode)
    }

    private fun DRI.displayable(): String = "${packageName}.${sureClassNames}"
}

