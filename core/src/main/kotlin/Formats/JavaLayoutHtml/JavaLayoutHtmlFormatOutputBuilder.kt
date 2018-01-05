package org.jetbrains.dokka.Formats

import kotlinx.html.*
import kotlinx.html.Entities.nbsp
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.*
import org.jetbrains.dokka.LanguageService.RenderMode.FULL
import org.jetbrains.dokka.LanguageService.RenderMode.SUMMARY
import org.jetbrains.dokka.NodeKind.Companion.classLike
import java.net.URI


class JavaLayoutHtmlFormatOutputBuilder(
        val output: Appendable,
        val languageService: LanguageService,
        val uriProvider: JavaLayoutHtmlUriProvider,
        val templateService: JavaLayoutHtmlTemplateService,
        val logger: DokkaLogger,
        val uri: URI
) {

    val htmlConsumer = output.appendHTML()

    val contentToHtmlBuilder = ContentToHtmlBuilder(uriProvider, uri)

    private fun <T> FlowContent.summaryNodeGroup(nodes: Iterable<T>, header: String, headerAsRow: Boolean = false, row: TBODY.(T) -> Unit) {
        if (nodes.none()) return
        if (!headerAsRow) {
            h2 { +header }
        }
        table {
            if (headerAsRow) thead { tr { td { h3 { +header } } } }
            tbody {
                nodes.forEach { node ->
                    row(node)
                }
            }
        }
    }

    fun FlowContent.metaMarkup(content: ContentNode) = with(contentToHtmlBuilder) {
        appendContent(content)
    }

    fun FlowContent.metaMarkup(content: List<ContentNode>) = with(contentToHtmlBuilder) {
        appendContent(content)
    }

    private fun TBODY.formatClassLikeRow(node: DocumentationNode) = tr {
        td { a(href = uriProvider.linkTo(node, uri)) { +node.simpleName() } }
        td { metaMarkup(node.summary) }
    }

    private fun FlowContent.modifiers(node: DocumentationNode) {
        for (modifier in node.details(NodeKind.Modifier)) {
            renderedSignature(modifier, SUMMARY)
        }
    }

    private fun FlowContent.shortFunctionParametersList(func: DocumentationNode) {
        val params = func.details(NodeKind.Parameter)
                .map { languageService.render(it, FULL) }
                .run {
                    drop(1).fold(listOfNotNull(firstOrNull())) { acc, node ->
                        acc + ContentText(", ") + node
                    }
                }
        metaMarkup(listOf(ContentText("(")) + params + listOf(ContentText(")")))
    }


    private fun TBODY.functionLikeSummaryRow(node: DocumentationNode) = tr {
        if (node.kind != NodeKind.Constructor) {
            td {
                modifiers(node)
                renderedSignature(node.detail(NodeKind.Type), SUMMARY)
            }
        }
        td {
            div {
                code {
                    a(href = uriProvider.linkTo(node, uri)) { +node.name }
                    shortFunctionParametersList(node)
                }
            }

            metaMarkup(node.summary)
        }
    }

    private fun TBODY.propertyLikeSummaryRow(node: DocumentationNode) = tr {
        td {
            modifiers(node)
            renderedSignature(node.detail(NodeKind.Type), SUMMARY)
        }
        td {
            div {
                code {
                    a(href = uriProvider.linkTo(node, uri)) { +node.name }
                }
            }

            metaMarkup(node.summary)
        }
    }

    private fun TBODY.nestedClassSummaryRow(node: DocumentationNode) = tr {
        td {
            modifiers(node)
        }
        td {
            div {
                code {
                    a(href = uriProvider.linkTo(node, uri)) { +node.name }
                }
            }

            metaMarkup(node.summary)
        }
    }

    private fun TBODY.inheritRow(entry: Map.Entry<DocumentationNode, List<DocumentationNode>>, summaryRow: TBODY.(DocumentationNode) -> Unit) = tr {
        td {
            val (from, nodes) = entry
            +"From class "
            a(href = uriProvider.linkTo(from.owner!!, uri)) { +from.qualifiedName() }
            table {
                tbody {
                    for (node in nodes) {
                        summaryRow(node)
                    }
                }
            }
        }
    }

    private fun FlowContent.renderedSignature(node: DocumentationNode, mode: LanguageService.RenderMode = SUMMARY) {
        metaMarkup(languageService.render(node, mode))
    }

    private fun FlowContent.fullFunctionDocs(node: DocumentationNode) {
        div {
            id = node.signatureForAnchor(logger)
            h3 { +node.name }
            pre { renderedSignature(node, FULL) }
            metaMarkup(node.content)
            for ((name, sections) in node.content.sections.groupBy { it.tag }) {
                table {
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

    private fun FlowContent.fullPropertyDocs(node: DocumentationNode) {
        fullFunctionDocs(node)
    }

    fun appendPackage(node: DocumentationNode) = templateService.composePage(
            listOf(node),
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +node.name }
                metaMarkup(node.content)
                summaryNodeGroup(node.members(NodeKind.Class), "Classes") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.Exception), "Exceptions") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.TypeAlias), "Type-aliases") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.AnnotationClass), "Annotations") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.Enum), "Enums") { formatClassLikeRow(it) }

                summaryNodeGroup(node.members(NodeKind.Function), "Top-level functions summary") { functionLikeSummaryRow(it) }
                summaryNodeGroup(node.members(NodeKind.Property), "Top-level properties summary") { propertyLikeSummaryRow(it) }


                fullDocs(node.members(NodeKind.Function), { h2 { +"Top-level functions" } }) { fullFunctionDocs(it) }
                fullDocs(node.members(NodeKind.Property), { h2 { +"Top-level properties" } }) { fullPropertyDocs(it) }
            }
    )

    fun FlowContent.classHierarchy(node: DocumentationNode) {

        val superclasses = generateSequence(node) { it.superclass }.toList().asReversed()
        table {
            superclasses.forEach {
                tr {
                    if (it != superclasses.first()) {
                        td {
                            +"   ↳"
                        }
                    }
                    td {
                        a(href = uriProvider.linkTo(it, uri)) { +it.qualifiedName() }
                    }
                }
            }
        }
    }

    private fun FlowContent.subclasses(inheritors: List<DocumentationNode>, direct: Boolean) {
        if (inheritors.isEmpty()) return
        div {
            table {
                thead {
                    tr {
                        td {
                            if (direct)
                                +"Known Direct Subclasses"
                            else
                                +"Known Indirect Subclasses"
                        }
                    }
                }
                tbody {
                    inheritors.forEach {
                        tr {
                            td {
                                a(href = uriProvider.linkTo(it, uri)) { +it.classNodeNameWithOuterClass() }
                            }
                            td {
                                metaMarkup(it.summary)
                            }
                        }
                    }
                }
            }
        }
    }

    fun appendClassLike(node: DocumentationNode) = templateService.composePage(
            listOf(node),
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +node.name }
                pre { renderedSignature(node, FULL) }
                classHierarchy(node)

                val inheritors = generateSequence(node.inheritors) { inheritors ->
                    inheritors
                            .flatMap { it.inheritors }
                            .takeUnless { it.isEmpty() }
                }
                subclasses(inheritors.first(), true)
                subclasses(inheritors.drop(1).flatten().toList(), false)


                metaMarkup(node.content)

                h2 { +"Summary" }

                fun DocumentationNode.isFunction() = kind == NodeKind.Function || kind == NodeKind.CompanionObjectFunction
                fun DocumentationNode.isProperty() = kind == NodeKind.Property || kind == NodeKind.CompanionObjectProperty

                val functionsToDisplay = node.members.filter(DocumentationNode::isFunction)
                val properties = node.members.filter(DocumentationNode::isProperty)
                val inheritedFunctionsByReceiver = node.inheritedMembers.filter(DocumentationNode::isFunction).groupBy { it.owner!! }
                val inheritedPropertiesByReceiver = node.inheritedMembers.filter(DocumentationNode::isProperty).groupBy { it.owner!! }
                val extensionProperties = node.extensions.filter(DocumentationNode::isProperty)
                val extensionFunctions = node.extensions.filter(DocumentationNode::isFunction)

                summaryNodeGroup(node.members.filter { it.kind in NodeKind.classLike }, "Nested classes", headerAsRow = true) { nestedClassSummaryRow(it) }

                summaryNodeGroup(node.members(NodeKind.Constructor), "Constructors", headerAsRow = true) { functionLikeSummaryRow(it) }

                summaryNodeGroup(functionsToDisplay, "Functions", headerAsRow = true) { functionLikeSummaryRow(it) }
                summaryNodeGroup(inheritedFunctionsByReceiver.entries, "Inherited functions", headerAsRow = true) { inheritRow(it) { functionLikeSummaryRow(it) } }
                summaryNodeGroup(extensionFunctions, "Extension functions", headerAsRow = true) { functionLikeSummaryRow(it) }


                summaryNodeGroup(properties, "Properties", headerAsRow = true) { propertyLikeSummaryRow(it) }
                summaryNodeGroup(inheritedPropertiesByReceiver.entries, "Inherited properties", headerAsRow = true) { inheritRow(it) { propertyLikeSummaryRow(it) } }
                summaryNodeGroup(extensionProperties, "Extension properties", headerAsRow = true) { propertyLikeSummaryRow(it) }

                fullDocs(node.members(NodeKind.Constructor), { h2 { +"Constructors" } }) { fullFunctionDocs(it) }
                fullDocs(functionsToDisplay, { h2 { +"Functions" } }) { fullFunctionDocs(it) }
                fullDocs(extensionFunctions, { h2 { +"Extension functions" } }) { fullFunctionDocs(it) }
                fullDocs(properties, { h2 { +"Properties" } }) { fullPropertyDocs(it) }
                fullDocs(extensionProperties, { h2 { +"Extension properties" } }) { fullPropertyDocs(it) }
            }
    )

    fun generateClassesIndex(allTypesNode: DocumentationNode) = templateService.composePage(
            listOf(allTypesNode),
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +"Class Index" }

                fun DocumentationNode.classWithNestedClasses(): List<DocumentationNode> =
                        members.filter { it.kind in classLike }.flatMap(DocumentationNode::classWithNestedClasses) + this

                val classesByFirstLetter = allTypesNode.members
                        .filterNot { it.kind == NodeKind.ExternalClass }
                        .flatMap(DocumentationNode::classWithNestedClasses)
                        .groupBy {
                            it.classNodeNameWithOuterClass().first().toString()
                        }
                        .entries
                        .sortedBy { (letter) -> letter }

                ul {
                    classesByFirstLetter.forEach { (letter) ->
                        li { a(href = "#letter_$letter") { +letter } }
                    }
                }

                classesByFirstLetter.forEach { (letter, nodes) ->
                    h2 {
                        id = "letter_$letter"
                        +letter
                    }
                    table {
                        tbody {
                            for (node in nodes.sortedBy { it.classNodeNameWithOuterClass() }) {
                                tr {
                                    td {
                                        a(href = uriProvider.linkTo(node, uri)) { +node.classNodeNameWithOuterClass() }
                                    }
                                    td {
                                        metaMarkup(node.content)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    )

    fun generatePackageIndex(nodes: List<DocumentationNode>) = templateService.composePage(nodes,
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +"Package Index" }
                table {
                    tbody {
                        for (node in nodes.sortedBy { it.name }) {
                            tr {
                                td {
                                    a(href = uriProvider.linkTo(node, uri)) { +node.name }
                                }
                                td {
                                    metaMarkup(node.content)
                                }
                            }
                        }
                    }
                }
            }
    )

    private fun FlowContent.fullDocs(
            nodes: List<DocumentationNode>,
            header: FlowContent.() -> Unit,
            renderNode: FlowContent.(DocumentationNode) -> Unit
    ) {
        if (nodes.none()) return
        header()
        for (node in nodes) {
            renderNode(node)
        }
    }
}

class ContentToHtmlBuilder(val uriProvider: JavaLayoutHtmlUriProvider, val uri: URI) {
    fun FlowContent.appendContent(content: List<ContentNode>): Unit = content.forEach { appendContent(it) }

    private fun FlowContent.hN(level: Int, classes: String? = null, block: CommonAttributeGroupFacadeFlowHeadingPhrasingContent.() -> Unit) {
        when (level) {
            1 -> h1(classes, block)
            2 -> h2(classes, block)
            3 -> h3(classes, block)
            4 -> h4(classes, block)
            5 -> h5(classes, block)
            6 -> h6(classes, block)
        }
    }

    fun FlowContent.appendContent(content: ContentNode) {
        when (content) {
            is ContentText -> +content.text
            is ContentSymbol -> span("symbol") { +content.text }
            is ContentKeyword -> span("keyword") { +content.text }
            is ContentIdentifier -> span("identifier") {
                content.signature?.let { id = it }
                +content.text
            }

            is ContentHeading -> hN(level = content.level) { appendContent(content.children) }

            is ContentEntity -> +content.text

            is ContentStrong -> strong { appendContent(content.children) }
            is ContentStrikethrough -> del { appendContent(content.children) }
            is ContentEmphasis -> em { appendContent(content.children) }

            is ContentOrderedList -> ol { appendContent(content.children) }
            is ContentUnorderedList -> ul { appendContent(content.children) }
            is ContentListItem -> consumer.li {
                (content.children.singleOrNull() as? ContentParagraph)
                        ?.let { paragraph -> appendContent(paragraph.children) }
                        ?: appendContent(content.children)
            }


            is ContentCode -> pre { code { appendContent(content.children) } }
            is ContentBlockSampleCode -> pre { code {} }
            is ContentBlockCode -> pre { code {} }


            is ContentNonBreakingSpace -> +nbsp
            is ContentSoftLineBreak, is ContentIndentedSoftLineBreak -> {
            }

            is ContentParagraph -> p { appendContent(content.children) }

            is ContentNodeLink -> {
                a(href = content.node?.let { uriProvider.linkTo(it, uri) }
                        ?: "#unresolved") { appendContent(content.children) }
            }
            is ContentExternalLink -> {
                a(href = content.href) { appendContent(content.children) }
            }

            is ContentBlock -> appendContent(content.children)
        }
    }
}