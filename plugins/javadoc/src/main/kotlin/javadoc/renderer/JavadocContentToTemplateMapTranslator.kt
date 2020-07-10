package javadoc.renderer

import javadoc.pages.*
import javadoc.toNormalized
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.ImplementedInterfaces
import org.jetbrains.dokka.model.InheritedFunction
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.nio.file.Paths

internal class JavadocContentToTemplateMapTranslator(
    private val locationProvider: LocationProvider,
    private val context: DokkaContext,
) {

    fun templateMapForPageNode(node: JavadocPageNode): TemplateMap =
        mapOf<String, Any?>(
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
            else -> emptyMap()
        }

    private fun pathToRoot(node: JavadocPageNode): String {
        return when(node){
            is JavadocModulePageNode -> ""
            else -> run {
                val link = locationProvider.resolve(node, skipExtension = true)
                val dir = Paths.get(link).parent?.toNormalized().orEmpty()
                return dir.split("/").filter { it.isNotEmpty() }.joinToString("/") { ".." }.let {
                    if (it.isNotEmpty()) "$it/" else it
                }
            }
        }
    }

    private inner class InnerTranslator(val contextNode: PageNode) {

        private val htmlTranslator = JavadocContentToHtmlTranslator(locationProvider, context)

        fun templateMapForAllClassesPage(node: AllClassesPage): TemplateMap {
            return mapOf(
                "title" to "All Classes",
                "list" to node.classEntries
            )
        }

        fun templateMapForTreeViewPage(node: TreeViewPage): TemplateMap {
            return mapOf(
                "title" to node.title,
                "name" to node.name,
                "kind" to node.kind,
                "list" to node.packages.orEmpty() + node.classes.orEmpty(),
                "classGraph" to node.classGraph,
                "interfaceGraph" to node.interfaceGraph
            )
        }

        fun templateMapForPackagePageNode(node: JavadocPackagePageNode): TemplateMap {
            return mapOf(
                "kind" to "package"
            ) + templateMapForJavadocContentNode(node.content)
        }

        fun templateMapForFunctionNode(node: JavadocFunctionNode): TemplateMap {
            return mapOf(
                "brief" to htmlForContentNodes(node.brief, contextNode),
                "parameters" to node.parameters.map { templateMapForParameterNode(it) },
                "inlineParameters" to node.parameters.joinToString { renderInlineParameter(it) },
                "signature" to templateMapForSignatureNode(node.signature),
                "name" to node.name
            )
        }

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

        fun templateMapForJavadocContentNode(node: JavadocContentNode): TemplateMap =
            when (node) {
                is TitleNode -> templateMapForTitleNode(node)
                is JavadocContentGroup -> templateMapForJavadocContentGroup(node)
                is LeafListNode -> templateMapForLeafListNode(node)
                is RootListNode -> templateMapForRootListNode(node)
                else -> emptyMap()
            }

        private fun templateMapForParameterNode(node: JavadocParameterNode): TemplateMap =
            mapOf(
                "description" to htmlForContentNodes(node.description, contextNode),
                "name" to node.name,
                "type" to htmlForContentNode(node.type, contextNode)
            )

        private fun templateMapForImplementedInterfaces(node: JavadocClasslikePageNode) =
            node.extras[ImplementedInterfaces]?.interfaces?.entries?.firstOrNull { it.key.analysisPlatform == Platform.jvm }?.value?.map { it.displayable() } // TODO: REMOVE HARDCODED JVM DEPENDENCY
                .orEmpty()

        private fun templateMapForClasslikeMethods(nodes: List<JavadocFunctionNode>): TemplateMap {
            val (inherited, own) = nodes.partition {
                val extra = it.extras[InheritedFunction]
                extra?.inheritedFrom?.keys?.firstOrNull { it.analysisPlatform == Platform.jvm }?.let { jvm ->
                    extra.isInherited(jvm)
                } ?: false
            }
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
            val inheritedFrom = node.extras[InheritedFunction]?.inheritedFrom
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

