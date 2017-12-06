package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import com.google.inject.Inject
import com.google.inject.name.Named
import kotlinx.html.*
import kotlinx.html.Entities.nbsp
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.*
import org.jetbrains.dokka.LanguageService.RenderMode.FULL
import org.jetbrains.dokka.LanguageService.RenderMode.SUMMARY
import org.jetbrains.dokka.NodeKind.Companion.classLike
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.toType
import org.jetbrains.kotlin.preprocessor.mkdirsOrFail
import java.io.File


class JavaLayoutHtmlFormatDescriptor : FormatDescriptor, DefaultAnalysisComponent, DefaultAnalysisComponentServices by KotlinAsKotlin {

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<Generator>() toType generatorServiceClass
        bind<LanguageService>() toType kotlinLanguageService
    }

    val generatorServiceClass = JavaLayoutHtmlFormatGenerator::class
    val kotlinLanguageService = KotlinLanguageService::class
}

class JavaLayoutHtmlFormatOutputBuilder(val output: Appendable, val languageService: LanguageService) {

    val htmlConsumer = output.appendHTML()

    val contentToHtmlBuilder = ContentToHtmlBuilder()

    private fun FlowContent.summaryNodeGroup(nodes: Iterable<DocumentationNode>, header: String, headerAsRow: Boolean = false, row: TBODY.(DocumentationNode) -> Unit) {
        if (nodes.none()) return
        if (!headerAsRow) h2 { +header }
        hr()
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
        td { a(href = "#classLocation") { +node.simpleName() } }
        td { metaMarkup(node.summary) }
    }

    private fun TBODY.formatFunctionSummaryRow(node: DocumentationNode) = tr {
        td {
            for (modifier in node.details(NodeKind.Modifier)) {
                metaMarkup(languageService.render(modifier, SUMMARY))
            }
            metaMarkup(languageService.render(node.detail(NodeKind.Type), SUMMARY))
        }
        td {
            div {
                a(href = "#${node.signature()}") { +node.name }
            }

            metaMarkup(node.summary)
        }
    }

    private fun FlowContent.fullFunctionDocs(node: DocumentationNode) {
        div {
            h3 { +node.name }
            pre { metaMarkup(languageService.render(node, FULL)) }
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
        a(name = node.signature())
    }

    fun appendPackage(node: DocumentationNode) = with(htmlConsumer) {
        html {
            head {}
            body {
                h1 { +node.name }
                metaMarkup(node.content)
                summaryNodeGroup(node.members(NodeKind.Class), "Classes") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.Exception), "Exceptions") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.TypeAlias), "Type-aliases") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.AnnotationClass), "Annotations") { formatClassLikeRow(it) }
                summaryNodeGroup(node.members(NodeKind.Enum), "Enums") { formatClassLikeRow(it) }

                summaryNodeGroup(node.members(NodeKind.Function), "Top-level functions summary") { formatFunctionSummaryRow(it) }


                h2 { +"Top-level functions" }
                hr()
                for (function in node.members(NodeKind.Function)) {
                    fullFunctionDocs(function)
                }
            }
        }
    }

    fun appendClassLike(node: DocumentationNode) = with(htmlConsumer) {

    }
}

class ContentToHtmlBuilder {
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
            is ContentBlock -> appendContent(content.children)

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
            is ContentBlockCode -> pre { code {} }
            is ContentBlockSampleCode -> pre { code {} }


            is ContentNonBreakingSpace -> +nbsp
            is ContentSoftLineBreak, is ContentIndentedSoftLineBreak -> {
            }

            is ContentParagraph -> p { appendContent(content.children) }

            is ContentNodeLink -> {
                a(href = "#local") { appendContent(content.children) }
            }
            is ContentExternalLink -> {
                a(href = "#external") { appendContent(content.children) }
            }
        }
    }
}

class JavaLayoutHtmlFormatGenerator @Inject constructor(@Named("outputDir") val root: File, val languageService: LanguageService) : Generator {


    fun buildClass(node: DocumentationNode, parentDir: File) {

    }

    fun buildPackage(node: DocumentationNode, parentDir: File) {
        assert(node.kind == NodeKind.Package)
        val members = node.members
        val directoryForPackage = parentDir.resolve(node.name)
        directoryForPackage.mkdirsOrFail()

        directoryForPackage.resolve("package-summary.html").bufferedWriter().use {
            JavaLayoutHtmlFormatOutputBuilder(it, languageService).appendPackage(node)
        }

        for (member in members) {
            when (member.kind) {
                NodeKind.Package -> buildPackage(member, directoryForPackage)
                in classLike -> buildClass(node, directoryForPackage)
                else -> {
                }
            }
        }
    }


    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single()

        module.members.filter { it.kind == NodeKind.Package }.forEach { buildPackage(it, root.resolve(module.name)) }
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {

    }

    override fun buildSupportFiles() {}

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {

    }
}


fun FlowOrInteractiveOrPhrasingContent.a(href: String? = null, target: String? = null, classes: String? = null, name: String? = null, block: A.() -> Unit = {}): Unit = A(attributesMapOf("href", href, "target", target, "class", classes, "name", name), consumer).visit(block)

fun DocumentationNode.signature() = detail(NodeKind.Signature).name