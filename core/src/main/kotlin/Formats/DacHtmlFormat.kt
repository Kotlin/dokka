package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import kotlinx.html.*
import org.jetbrains.dokka.*
import kotlin.reflect.KClass

/**
 * Data structure used for generating `data-reference-resources-wrapper`.
 */
val nodeToFamilyMap = HashMap<DocumentationNode, List<DocumentationNode>>()

/**
 * On Devsite, certain headers and footers are needed for generating Devsite metadata.
 */
class DevsiteHtmlTemplateService @Inject constructor(
        val uriProvider: JavaLayoutHtmlUriProvider
) : JavaLayoutHtmlTemplateService {
    override fun composePage(
            nodes: List<DocumentationNode>,
            tagConsumer: TagConsumer<Appendable>,
            headContent: HEAD.() -> Unit,
            bodyContent: BODY.() -> Unit
    ) {
        tagConsumer.html {
            attributes["devsite"] = "true"
            head {
                headContent()
                meta(name = "top_category") { attributes["value"] = "develop" }
                meta(name = "subcategory") { attributes["value"] = "reference" }
                meta(name = "hide_page_heading") { attributes["value"] = "true" }
                meta(name = "book_path") { attributes["value"] = "/reference/kotlin/_toc.yaml" }
                meta(name = "project_path") { attributes["value"] = "/reference/_project.yaml" }
            }
            body {
                bodyContent()
                // TODO Refactor appendDataReferenceResourceWrapper to use KotlinX.HTML
                unsafe { raw(StringBuilder().appendDataReferenceResourceWrapper(nodes).toString()) }
            }
        }
    }


    /**
     * Appends `data-reference-resources-wrapper` data to the body of the page. This is required
     * for highlighting the current page in the left nav of DAC.
     */
    private fun Appendable.appendDataReferenceResourceWrapper(nodes: Iterable<DocumentationNode>) {
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
                appendln("<div class=\"data-reference-resources-wrapper\">")
                appendln("  <ul data-reference-resources>")
                val interfaceFamily = family[NodeKind.Interface]
                if (interfaceFamily != null) {
                    appendln("    <li><h2>Interfaces</h2>")
                    appendFamily(pageOwner, interfaceFamily)
                }
                val classFamily = family[NodeKind.Class]
                if (classFamily != null) {
                    appendln("    <li><h2>Classes</h2>")
                    appendFamily(pageOwner, classFamily)
                }
                val enumFamily = family[NodeKind.Enum]
                if (enumFamily != null) {
                    appendln("    <li><h2>Enums</h2>")
                    appendFamily(pageOwner, enumFamily)
                }
                val annotationFamily = family[NodeKind.AnnotationClass]
                if (annotationFamily != null) {
                    appendln("    <li><h2>Annotations</h2>")
                    appendFamily(pageOwner, annotationFamily)
                }
                val exceptionFamily = family[NodeKind.Exception]
                if (exceptionFamily != null) {
                    appendln("    <li><h2>Exceptions</h2>")
                    appendFamily(pageOwner, exceptionFamily)
                }
                val objectFamily = family[NodeKind.Object]
                if (objectFamily != null) {
                    appendln("    <li><h2>Objects</h2>")
                    appendFamily(pageOwner, objectFamily)
                }
                appendln("  </ul>")
                appendln("</div>")
            }
        }
    }

    /**
     * Formats the `family` of the node for the `data-reference-resources-wrapper`.
     * TODO: investigate expressing apilevel.
     */
    private fun Appendable.appendFamily(selectedNode: DocumentationNode, family: List<DocumentationNode>) {
        appendln("      <ul>")
        for (node in family) {
            val selected = if (node == selectedNode) "selected " else ""
            appendln("          <li class=\"${selected}api apilevel-\">" +
                    "<a href=\"/${uriProvider.mainUriOrWarn(node)}\">${node.nameWithOuterClass()}</a></li>")
        }
        appendln("      </ul>")
    }
}

class DacFormatDescriptor : JavaLayoutHtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsKotlin {
    override val templateServiceClass: KClass<out JavaLayoutHtmlTemplateService> = DevsiteHtmlTemplateService::class

    override val outlineFactoryClass = DacOutlineFormatter::class
    override val languageServiceClass = KotlinLanguageService::class
}