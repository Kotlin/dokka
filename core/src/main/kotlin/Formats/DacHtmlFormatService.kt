package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName
import java.nio.file.Path

/**
 * Data structure used for generating `data-reference-resources-wrapper`.
 */
val nodeToFamilyMap = HashMap<DocumentationNode, List<DocumentationNode>>()

/**
 * On Devsite, certain headers and footers are needed for generating Devsite metadata.
 */
class DevsiteHtmlTemplateService : HtmlTemplateService {
    override fun appendFooter(to: StringBuilder) {
        if (!to.endsWith('\n')) {
            to.append('\n')
        }
        to.appendln("</body>")
        to.appendln("</html>")
    }

    /**
     * Header required for rendering left-nav properly in Devsite.
     *
     * TODO: Add book path and project path as dependencies; remove hardcoded paths.
     */
    override fun appendHeader(to: StringBuilder, title: String?, basePath: Path) {
        to.appendln("<html devsite>")
        to.appendln("<head>")
        if (title != null) {
            to.appendln("  <title>$title</title>")
        }
        to.appendln("  <meta name=\"top_category\" value=\"develop\" />")
        to.appendln("  <meta name=\"subcategory\" value=\"reference\" />")
        to.appendln("  <meta name=\"hide_page_heading\" value=\"true\" />")
        to.appendln("  <meta name=\"book_path\" value=\"/reference/kotlin/_toc.yaml\" />")
        to.appendln("  <meta name=\"project_path\" value=\"/reference/_project.yaml\" />")
        to.appendln("</head>")
        to.appendln("<body>")
    }
}

/**
 * Override the page builders so the class names in the page headers include the outer class.
 * For example, a header for View.OnClickListener would be `View.OnClickListener` instead of
 * `OnClickListener.
 *
 * Also, appends devsite-specific `data-reference-resources-wrapper` data after the body and before
 * the footer to each page.
 */
class DacHtmlOutputBuilder(to: StringBuilder,
                           location: Location,
                           locationService: LocationService,
                           languageService: LanguageService,
                           extension: String,
                           impliedPlatforms: List<String>
) : HtmlOutputBuilder(to, location, locationService, languageService, extension, impliedPlatforms,
        DevsiteHtmlTemplateService()) {

    inner class DacPageBuilder(
            nodes: Iterable<DocumentationNode>,
            noHeader: Boolean = false
    ) : PageBuilder(nodes, noHeader) {

        override fun getNameForHeader(node: DocumentationNode): String {
            return node.nameWithOuterClass()
        }
    }

    inner class SingleNodeDacPageBuilder(node: DocumentationNode, noHeader: Boolean = false)
        : SingleNodePageBuilder(node, noHeader) {
        override fun getNameForHeader(node: DocumentationNode): String {
            // If the class is an inner class or interface, the name is
            return node.nameWithOuterClass()
        }
    }

    inner class AllTypesNodeDacBuilder(node: DocumentationNode)
        : AllTypesNodeBuilder(node) {
        override fun getNameForHeader(node: DocumentationNode): String {
            return node.nameWithOuterClass()
        }
    }

    inner class GroupNodeDacPageBuilder(node: DocumentationNode) : GroupNodePageBuilder(node) {
        override fun getNameForHeader(node: DocumentationNode): String {
            return node.nameWithOuterClass()
        }
    }

    override fun appendNodes(nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to, getPageTitle(nodes), locationService.calcPathToRoot(location))
        val singleNode = nodes.singleOrNull()
        when (singleNode?.kind) {
            NodeKind.AllTypes -> AllTypesNodeDacBuilder(singleNode).build()
            NodeKind.GroupNode -> GroupNodeDacPageBuilder(singleNode).build()
            null -> DacPageBuilder(nodes).build()
            else -> SingleNodeDacPageBuilder(singleNode).build()
        }
        appendDataReferenceResourceWrapper(nodes)
        templateService.appendFooter(to)
    }

    /**
     * Appends `data-reference-resources-wrapper` data to the body of the page. This is required
     * for highlighting the current page in the left nav of DAC.
     */
    private fun appendDataReferenceResourceWrapper(nodes: Iterable<DocumentationNode>) {
        val node = nodes.first()
        if (node.isPackage()) {
            val children = node.getMembersOfKinds(NodeKind.Class, NodeKind.Interface, NodeKind.Enum,
                    NodeKind.AnnotationClass, NodeKind.Exception, NodeKind.Object)
            for (child in children) {
                nodeToFamilyMap.put(child, children)
            }
        } else if (node.hasOwnPage() || node.kind in NodeKind.memberLike) {
            val pageOwner = node.pageOwner()
            val family = nodeToFamilyMap[pageOwner]?.groupBy { it.kind }
            if (family != null) {
                to.appendln("<div class=\"data-reference-resources-wrapper\">")
                to.appendln("  <ul data-reference-resources>")
                val interfaceFamily = family[NodeKind.Interface]
                if (interfaceFamily != null) {
                    to.appendln("    <li><h2>Interfaces</h2>")
                    to.appendFamily(pageOwner, interfaceFamily)
                }
                val classFamily = family[NodeKind.Class]
                if (classFamily != null) {
                    to.appendln("    <li><h2>Classes</h2>")
                    to.appendFamily(pageOwner, classFamily)
                }
                val enumFamily = family[NodeKind.Enum]
                if (enumFamily != null) {
                    to.appendln("    <li><h2>Enums</h2>")
                    to.appendFamily(pageOwner, enumFamily)
                }
                val annotationFamily = family[NodeKind.AnnotationClass]
                if (annotationFamily != null) {
                    to.appendln("    <li><h2>Annotations</h2>")
                    to.appendFamily(pageOwner, annotationFamily)
                }
                val exceptionFamily = family[NodeKind.Exception]
                if (exceptionFamily != null) {
                    to.appendln("    <li><h2>Exceptions</h2>")
                    to.appendFamily(pageOwner, exceptionFamily)
                }
                val objectFamily = family[NodeKind.Object]
                if (objectFamily != null) {
                    to.appendln("    <li><h2>Objects</h2>")
                    to.appendFamily(pageOwner, objectFamily)
                }
                to.appendln("  </ul>")
                to.appendln("</div>")
            }
        }
    }

    /**
     * Formats the `family` of the node for the `data-reference-resources-wrapper`.
     * TODO: investigate expressing apilevel.
     */
    private fun StringBuilder.appendFamily(selectedNode: DocumentationNode, family: List<DocumentationNode>) {
        appendln("      <ul>")
        for (node in family) {
            val selected = if (node == selectedNode) "selected " else ""
            appendln("          <li class=\"${selected}api apilevel-\">" +
                    "<a href=\"/${locationService.location(node).path}\">${node.nameWithOuterClass()}</a></li>")
        }
        appendln("      </ul>")
    }
}

/**
 * HtmlFormatService implementation that uses the [DevsiteHtmlTemplateService] and
 * [DacHtmlOutputBuilder].
 */
class DacHtmlFormatService @Inject constructor(locationService: LocationService,
                                               signatureGenerator: LanguageService,
                                               @Named(impliedPlatformsName) impliedPlatforms: List<String>)
    : HtmlFormatService(locationService, signatureGenerator, DevsiteHtmlTemplateService(), impliedPlatforms) {

    override fun enumerateSupportFiles(callback: (String, String) -> Unit) {}

    override fun createOutputBuilder(to: StringBuilder, location: Location) =
            DacHtmlOutputBuilder(to, location, locationService, languageService, extension,
                    impliedPlatforms)

}