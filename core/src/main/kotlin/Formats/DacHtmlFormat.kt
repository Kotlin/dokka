package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import com.google.inject.name.Named
import kotlinx.html.*
import org.jetbrains.dokka.*
import java.io.File
import java.net.URI
import kotlin.reflect.KClass

/**
 * Data structure used for generating `data-reference-resources-wrapper`.
 */
val nodeToFamilyMap = HashMap<DocumentationNode, List<DocumentationNode>>()

/**
 * On Devsite, certain headers and footers are needed for generating Devsite metadata.
 */
class DevsiteHtmlTemplateService @Inject constructor(
        val uriProvider: JavaLayoutHtmlUriProvider, @Named("outputDir") val rootFile: File
) : JavaLayoutHtmlTemplateService {
    override fun composePage(page: JavaLayoutHtmlFormatOutputBuilder.Page, tagConsumer: TagConsumer<Appendable>, headContent: HEAD.() -> Unit, bodyContent: BODY.() -> Unit) {
        tagConsumer.html {
            attributes["devsite"] = "true"
            head {
                headContent()
                title {+when(page) {
                    is JavaLayoutHtmlFormatOutputBuilder.Page.ClassIndex -> "Class Index | Android Developers"
                    is JavaLayoutHtmlFormatOutputBuilder.Page.ClassPage -> page.node.nameWithOuterClass()
                    is JavaLayoutHtmlFormatOutputBuilder.Page.PackageIndex -> "Package Index | Android Developers"
                    is JavaLayoutHtmlFormatOutputBuilder.Page.PackagePage -> page.node.nameWithOuterClass()
                }}
                unsafe {+"{% setvar book_path %}/reference/android/arch/_book.yaml{% endsetvar %}\n{% include \"_shared/_reference-head-tags.html\" %}\n"}
            }
            body {
                bodyContent()
                // TODO Refactor appendDataReferenceResourceWrapper to use KotlinX.HTML
                unsafe { raw(buildString { appendDataReferenceResourceWrapper(when(page) {
                    is JavaLayoutHtmlFormatOutputBuilder.Page.ClassIndex -> page.classes
                    is JavaLayoutHtmlFormatOutputBuilder.Page.ClassPage -> listOf(page.node)
                    is JavaLayoutHtmlFormatOutputBuilder.Page.PackageIndex -> page.packages
                    is JavaLayoutHtmlFormatOutputBuilder.Page.PackagePage -> listOf(page.node)
                }) }) }
            }
        }
    }

    /**
     * Appends `data-reference-resources-wrapper` data to the body of the page. This is required
     * for highlighting the current page in the left nav of DAC.
     */
    private fun Appendable.appendDataReferenceResourceWrapper(nodes: Iterable<DocumentationNode>) {
        if (nodes.none()) {
            return
        }
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

class DevsiteLayoutHtmlFormatOutputBuilderFactoryImpl @javax.inject.Inject constructor(
        val uriProvider: JavaLayoutHtmlUriProvider,
        val languageService: LanguageService,
        val templateService: JavaLayoutHtmlTemplateService,
        val logger: DokkaLogger
) : JavaLayoutHtmlFormatOutputBuilderFactory {
    override fun createOutputBuilder(output: Appendable, node: DocumentationNode): JavaLayoutHtmlFormatOutputBuilder {
        return createOutputBuilder(output, uriProvider.mainUri(node))
    }

    override fun createOutputBuilder(output: Appendable, uri: URI): JavaLayoutHtmlFormatOutputBuilder {
        return DevsiteLayoutHtmlFormatOutputBuilder(output, languageService, uriProvider, templateService, logger, uri)
    }
}

class DevsiteLayoutHtmlFormatOutputBuilder(
        output: Appendable,
        languageService: LanguageService,
        uriProvider: JavaLayoutHtmlUriProvider,
        templateService: JavaLayoutHtmlTemplateService,
        logger: DokkaLogger,
        uri: URI
) : JavaLayoutHtmlFormatOutputBuilder(output, languageService, uriProvider, templateService, logger, uri) {
    override fun FlowContent.fullMemberDocs(node: DocumentationNode) {
        div {
            id = node.signatureForAnchor(logger)
            h3(classes = "api-name") { +node.name }
            div(classes="api-level") {
                node.apiLevel?.let {
                    +"added in "
                    a(href = "https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels")
                    +"API level ${it.name}"
                }
            }
            pre(classes = "api-signature no-pretty-print") { renderedSignature(node, LanguageService.RenderMode.FULL) }
            contentNodeToMarkup(node.content)
            node.constantValue()?.let { value ->
                pre {
                    +"Value: "
                    code { +value }
                }
            }
            for ((name, sections) in node.content.sections.groupBy { it.tag }) {
                table(classes = "responsive") {
                    thead { tr { td { h3 { +name } } } }
                    tbody {
                        sections.forEach {
                            tr {
                                td { it.subjectName?.let { +it } }
                                td {
                                    metaMarkup(it.children)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun FlowContent.classLikeSummaries(page: Page.ClassPage) = with(page) {
        summaryNodeGroup(
                nestedClasses,
                "Nested classes",
                headerAsRow = true
        ) {
            nestedClassSummaryRow(it)
        }

        summaryNodeGroup(attributes, "XML attributes") { propertyLikeSummaryRow(it) }

        summaryNodeGroup(constants, "Constants") { propertyLikeSummaryRow(it) }

        summaryNodeGroup(
                constructors,
                "Constructors",
                headerAsRow = true
        ) {
            functionLikeSummaryRow(it)
        }

        summaryNodeGroup(functions, "Functions", headerAsRow = true) { functionLikeSummaryRow(it) }
        summaryNodeGroup(
                companionFunctions,
                "Companion functions",
                headerAsRow = true
        ) {
            functionLikeSummaryRow(it)
        }
        summaryNodeGroup(
                inheritedFunctionsByReceiver.entries,
                "Inherited functions",
                headerAsRow = true
        ) {
            inheritRow(it) {
                functionLikeSummaryRow(it)
            }
        }
        summaryNodeGroup(
                extensionFunctions.entries,
                "Extension functions",
                headerAsRow = true
        ) {
            extensionRow(it) {
                functionLikeSummaryRow(it)
            }
        }
        summaryNodeGroup(
                inheritedExtensionFunctions.entries,
                "Inherited extension functions",
                headerAsRow = true
        ) {
            extensionRow(it) {
                functionLikeSummaryRow(it)
            }
        }


        summaryNodeGroup(properties, "Properties", headerAsRow = true) { propertyLikeSummaryRow(it) }
        summaryNodeGroup(
                companionProperties,
                "Companion properties",
                headerAsRow = true
        ) {
            propertyLikeSummaryRow(it)
        }

        summaryNodeGroup(
                inheritedPropertiesByReceiver.entries,
                "Inherited properties",
                headerAsRow = true
        ) {
            inheritRow(it) {
                propertyLikeSummaryRow(it)
            }
        }
        summaryNodeGroup(
                extensionProperties.entries,
                "Extension properties",
                headerAsRow = true
        ) {
            extensionRow(it) {
                propertyLikeSummaryRow(it)
            }
        }
        summaryNodeGroup(
                inheritedExtensionProperties.entries,
                "Inherited extension properties",
                headerAsRow = true
        ) {
            extensionRow(it) {
                propertyLikeSummaryRow(it)
            }
        }
    }
}

class DacFormatDescriptor : JavaLayoutHtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsKotlin {
    override val templateServiceClass: KClass<out JavaLayoutHtmlTemplateService> = DevsiteHtmlTemplateService::class

    override val outlineFactoryClass = DacOutlineFormatter::class
    override val languageServiceClass = KotlinLanguageService::class
    override val packageListServiceClass: KClass<out PackageListService> = JavaLayoutHtmlPackageListService::class
    override val outputBuilderFactoryClass: KClass<out JavaLayoutHtmlFormatOutputBuilderFactory> = DevsiteLayoutHtmlFormatOutputBuilderFactoryImpl::class
}