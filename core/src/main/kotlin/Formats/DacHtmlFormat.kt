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
        val uriProvider: JavaLayoutHtmlUriProvider, @Named("outputDir") val rootFile: File,
        @Named("outlineRoot") val outlineRoot: String, @Named("dacRoot") val dacRoot: String
) : JavaLayoutHtmlTemplateService {
    override fun composePage(page: JavaLayoutHtmlFormatOutputBuilder.Page, tagConsumer: TagConsumer<Appendable>, headContent: HEAD.() -> Unit, bodyContent: BODY.() -> Unit) {
//        System.out.println("dacRoot=$dacRoot")
        tagConsumer.html {
            attributes["devsite"] = "true"
            head {
                headContent()
                title {
                    +when (page) {
                        is JavaLayoutHtmlFormatOutputBuilder.Page.ClassIndex -> "Class Index | Android Developers"
                        is JavaLayoutHtmlFormatOutputBuilder.Page.ClassPage -> page.node.nameWithOuterClass()
                        is JavaLayoutHtmlFormatOutputBuilder.Page.PackageIndex -> "Package Index | Android Developers"
                        is JavaLayoutHtmlFormatOutputBuilder.Page.PackagePage -> page.node.nameWithOuterClass()
                    }
                }
                unsafe { +"{% setvar book_path %}${dacRoot}/${outlineRoot}_book.yaml{% endsetvar %}\n{% include \"_shared/_reference-head-tags.html\" %}\n" }
            }
            body {
                bodyContent()
                // TODO Refactor appendDataReferenceResourceWrapper to use KotlinX.HTML
                unsafe {
                    raw(buildString {
                        appendDataReferenceResourceWrapper(when (page) {
                            is JavaLayoutHtmlFormatOutputBuilder.Page.ClassIndex -> page.classes
                            is JavaLayoutHtmlFormatOutputBuilder.Page.ClassPage -> listOf(page.node)
                            is JavaLayoutHtmlFormatOutputBuilder.Page.PackageIndex -> page.packages
                            is JavaLayoutHtmlFormatOutputBuilder.Page.PackagePage -> listOf(page.node)
                        })
                    })
                }
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
        a {
            attributes["name"] = node.signatureForAnchor(logger).urlEncoded()
        }
        div(classes = "api apilevel-${node.apiLevel.name}") {
            attributes["data-version-added"] = node.apiLevel.name
            h3(classes = "api-name") {
//                id = node.signatureForAnchor(logger).urlEncoded()
                +node.name
            }
            div(classes = "api-level") {
                +"added in "
                a(href = "https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels") {
                    +"API level ${node.apiLevel.name}"
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
                when (name) {
                    ContentTags.Return -> {
                        table(classes = "responsive") {
                            tbody {
                                tr {
                                    th {
                                        colSpan = "2"
                                        +name
                                    }
                                }
                                sections.forEach {
                                    tr {
                                        td {
                                            colSpan = "2"
                                            metaMarkup(it.children)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ContentTags.Parameters -> {
                        table(classes = "responsive") {
                            tbody {
                                tr {
                                    th {
                                        colSpan = "2"
                                        +name
                                    }
                                }
                                sections.forEach {
                                    tr {
                                        td {
                                            code {
                                                it.subjectName?.let { +it }
                                            }
                                        }
                                        td {
                                            metaMarkup(it.children)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ContentTags.SeeAlso -> {
                        div {
                            p {
                                b {
                                    +name
                                }
                            }
                            ul(classes = "nolist") {
                                sections.forEach {
                                    li {
                                        code {
                                            metaMarkup(it.children)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ContentTags.Exceptions -> {
                        table(classes = "responsive") {
                            tbody {
                                tr {
                                    th {
                                        colSpan = "2"
                                        +name
                                    }
                                }
                                sections.forEach {
                                    tr {
                                        td {
                                            code {
                                                it.subjectName?.let { +it }
                                            }
                                        }
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
        }
    }

    fun TBODY.xmlAttributeRow(node: DocumentationNode) = tr {
        td {
            div {
                code {
                    +node.name
                }
            }

            val referencedElement = node.attributesLink.first()
            contentNodeToMarkup(referencedElement.summary)
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

        summaryNodeGroup(attributes, "XML attributes") { xmlAttributeRow(it) }

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

    override fun generatePackage(page: Page.PackagePage) = templateService.composePage(
            page,
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +page.node.name }
                contentNodeToMarkup(page.node.content)
                summaryNodeGroup(page.classes, "Classes", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.exceptions, "Exceptions", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.typeAliases, "Type-aliases", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.annotations, "Annotations", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.enums, "Enums", headerAsRow = false) { classLikeRow(it) }

                summaryNodeGroup(
                        page.constants,
                        "Top-level constants summary",
                        headerAsRow = false
                ) {
                    propertyLikeSummaryRow(it)
                }

                summaryNodeGroup(
                        page.functions,
                        "Top-level functions summary",
                        headerAsRow = false
                ) {
                    functionLikeSummaryRow(it)
                }

                summaryNodeGroup(
                        page.properties,
                        "Top-level properties summary",
                        headerAsRow = false
                ) {
                    propertyLikeSummaryRow(it)
                }

                summaryNodeGroupForExtensions("Extension functions summary", page.extensionFunctions.entries)
                summaryNodeGroupForExtensions("Extension properties summary", page.extensionProperties.entries)

                fullMemberDocs(page.constants, "Top-level constants")
                fullMemberDocs(page.functions, "Top-level functions")
                fullMemberDocs(page.properties, "Top-level properties")
                fullMemberDocs(page.extensionFunctions.values.flatten(), "Extension functions")
                fullMemberDocs(page.extensionProperties.values.flatten(), "Extension properties")
            }
    )

    private fun FlowContent.summaryNodeGroupForExtensions(
            header: String,
            receivers: Set<Map.Entry<DocumentationNode, List<DocumentationNode>>>
    ) {
        if (receivers.none()) return
        h2 { +header }
        div {
            receivers.forEach {
                table {
                    tr {
                        td {
                            attributes["colSpan"] = "2"
                            +"For "
                            a(href = it.key) { +it.key.name }
                        }
                    }
                    it.value.forEach { node ->
                        tr {
                            if (node.kind != NodeKind.Constructor) {
                                td {
                                    modifiers(node)
                                    renderedSignature(node.detail(NodeKind.Type), LanguageService.RenderMode.SUMMARY)
                                }
                            }
                            td {
                                div {
                                    code {
                                        val receiver = node.detailOrNull(NodeKind.Receiver)
                                        if (receiver != null) {
                                            renderedSignature(receiver.detail(NodeKind.Type), LanguageService.RenderMode.SUMMARY)
                                            +"."
                                        }
                                        a(href = node) { +node.name }
                                        shortFunctionParametersList(node)
                                    }
                                }

                                contentNodeToMarkup(node.summary)
                            }
                        }
                    }
                }
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


class DacAsJavaFormatDescriptor : JavaLayoutHtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsJava {
    override val templateServiceClass: KClass<out JavaLayoutHtmlTemplateService> = DevsiteHtmlTemplateService::class

    override val outlineFactoryClass = DacOutlineFormatter::class
    override val languageServiceClass = NewJavaLanguageService::class
    override val packageListServiceClass: KClass<out PackageListService> = JavaLayoutHtmlPackageListService::class
    override val outputBuilderFactoryClass: KClass<out JavaLayoutHtmlFormatOutputBuilderFactory> = DevsiteLayoutHtmlFormatOutputBuilderFactoryImpl::class
}