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
import org.jetbrains.dokka.NodeKind.Companion.memberLike
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.toType
import org.jetbrains.kotlin.preprocessor.mkdirsOrFail
import java.io.BufferedWriter
import java.io.File
import java.net.URI
import java.net.URLEncoder
import kotlin.reflect.KClass


abstract class JavaLayoutHtmlFormatDescriptorBase : FormatDescriptor, DefaultAnalysisComponent {

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<Generator>() toType generatorServiceClass
        bind<LanguageService>() toType languageServiceClass
        bind<JavaLayoutHtmlTemplateService>() toType templateServiceClass
    }

    val generatorServiceClass = JavaLayoutHtmlFormatGenerator::class
    abstract val languageServiceClass: KClass<out LanguageService>
    abstract val templateServiceClass: KClass<out JavaLayoutHtmlTemplateService>
    abstract val outlineFactoryClass: KClass<out JavaLayoutHtmlFormatOutlineFactoryService>?
}

class JavaLayoutHtmlFormatDescriptor : JavaLayoutHtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsKotlin {
    override val languageServiceClass = KotlinLanguageService::class
    override val templateServiceClass = JavaLayoutHtmlTemplateService.Default::class
    override val outlineFactoryClass = null
}


interface JavaLayoutHtmlTemplateService {
    fun composePage(
            nodes: List<DocumentationNode>,
            tagConsumer: TagConsumer<Appendable>,
            headContent: HEAD.() -> Unit,
            bodyContent: BODY.() -> Unit
    )

    class Default : JavaLayoutHtmlTemplateService {
        override fun composePage(
                nodes: List<DocumentationNode>,
                tagConsumer: TagConsumer<Appendable>,
                headContent: HEAD.() -> Unit,
                bodyContent: BODY.() -> Unit
        ) {
            tagConsumer.html {
                head(headContent)
                body(block = bodyContent)
            }
        }
    }
}

interface JavaLayoutHtmlFormatOutlineFactoryService {
    fun generateOutlines(outputProvider: (URI) -> Appendable, nodes: Iterable<DocumentationNode>)
}

class JavaLayoutHtmlFormatOutputBuilder(
        val output: Appendable,
        val languageService: LanguageService,
        val generator: JavaLayoutHtmlFormatGenerator,
        val templateService: JavaLayoutHtmlTemplateService,
        val uri: URI
) {

    val htmlConsumer = output.appendHTML()

    val contentToHtmlBuilder = ContentToHtmlBuilder(generator, uri)

    private fun FlowContent.summaryNodeGroup(nodes: Iterable<DocumentationNode>, header: String, headerAsRow: Boolean = false, row: TBODY.(DocumentationNode) -> Unit) {
        if (nodes.none()) return
        if (!headerAsRow) {
            h2 { +header }
            hr()
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
        td { a(href = generator.linkTo(node, uri)) { +node.simpleName() } }
        td { metaMarkup(node.summary) }
    }

    private fun TBODY.formatFunctionSummaryRow(node: DocumentationNode) = tr {
        td {
            for (modifier in node.details(NodeKind.Modifier)) {
                renderedSignature(modifier, SUMMARY)
            }
            renderedSignature(node.detail(NodeKind.Type), SUMMARY)
        }
        td {
            div {
                a(href = generator.linkTo(node, uri)) { +node.name }
            }

            metaMarkup(node.summary)
        }
    }

    private fun FlowContent.renderedSignature(node: DocumentationNode, mode: LanguageService.RenderMode = SUMMARY) {
        metaMarkup(languageService.render(node, mode))
    }

    private fun FlowContent.fullFunctionDocs(node: DocumentationNode) {
        div {
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
        a { id = node.signature() }
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

                summaryNodeGroup(node.members(NodeKind.Function), "Top-level functions summary") { formatFunctionSummaryRow(it) }


                h2 { +"Top-level functions" }
                hr()
                for (function in node.members(NodeKind.Function)) {
                    fullFunctionDocs(function)
                }
            }
    )


    fun appendClassLike(node: DocumentationNode) = templateService.composePage(
            listOf(node),
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +node.name }
                pre { renderedSignature(node, FULL) }
                metaMarkup(node.content)

                h2 { +"Summary" }
                hr()

                val functionsToDisplay = node.members(NodeKind.Function) + node.members(NodeKind.CompanionObjectFunction)

                summaryNodeGroup(functionsToDisplay, "Functions", headerAsRow = true) { formatFunctionSummaryRow(it) }

                h2 { +"Functions" }
                hr()
                for (function in functionsToDisplay) {
                    fullFunctionDocs(function)
                }
            }
    )
}

class ContentToHtmlBuilder(val generator: JavaLayoutHtmlFormatGenerator, val uri: URI) {
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
                a(href = generator.linkTo(content.node!!, uri)) { appendContent(content.children) }
            }
            is ContentExternalLink -> {
                a(href = content.href) { appendContent(content.children) }
            }

            is ContentBlock -> appendContent(content.children)
        }
    }
}

class JavaLayoutHtmlFormatGenerator @Inject constructor(
        @Named("outputDir") val root: File,
        val languageService: LanguageService,
        val templateService: JavaLayoutHtmlTemplateService,
        val outlineFactoryService: JavaLayoutHtmlFormatOutlineFactoryService
) : Generator {

    fun createOutputBuilderForNode(node: DocumentationNode, output: Appendable)
            = JavaLayoutHtmlFormatOutputBuilder(output, languageService, this, templateService, mainUriForNode(node))

    fun containerUriOfNode(node: DocumentationNode): URI {
        return when (node.kind) {
            NodeKind.Module -> URI("/").resolve(node.name)
            NodeKind.Package -> containerUriOfNode(node.owner!!).resolve(node.name.replace('.', '/') + '/')
            in classLike -> containerUriOfNode(node.owner!!).resolve("${node.name}.html")
            else -> error("Can't contain nested")
        }
    }

    fun mainUriForNode(node: DocumentationNode): URI {
        return when (node.kind) {
            NodeKind.Package -> containerUriOfNode(node).resolve("package-summary.html")
            NodeKind.Class -> containerUriOfNode(node).resolve("#")
            in memberLike -> mainUriForNode(node.owner!!).resolve("#${node.signatureUrlEncoded()}")
            else -> error("Not supported")
        }
    }

    fun linkTo(to: DocumentationNode, from: URI): String {
        return mainUriForNode(to).relativeTo(from).toString()
    }

    fun buildClass(node: DocumentationNode, parentDir: File) {
        val fileForClass = parentDir.resolve(node.simpleName() + ".html")
        fileForClass.bufferedWriter().use {
            createOutputBuilderForNode(node, it).appendClassLike(node)
        }
    }

    fun buildPackage(node: DocumentationNode, parentDir: File) {
        assert(node.kind == NodeKind.Package)
        val members = node.members
        val directoryForPackage = parentDir.resolve(node.name.replace('.', File.separatorChar))
        directoryForPackage.mkdirsOrFail()

        directoryForPackage.resolve("package-summary.html").bufferedWriter().use {
            createOutputBuilderForNode(node, it).appendPackage(node)
        }

        members.filter { it.kind in classLike }.forEach {
            buildClass(it, directoryForPackage)
        }
    }


    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single()

        val moduleRoot = root.resolve(module.name)
        module.members.filter { it.kind == NodeKind.Package }.forEach { buildPackage(it, moduleRoot) }
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        val uriToWriter = mutableMapOf<URI, BufferedWriter>()

        fun provideOutput(uri: URI): BufferedWriter {
            val normalized = uri.normalize()
            uriToWriter[normalized]?.let { return it }
            val file = root.resolve(normalized.path.removePrefix("/"))
            val writer = file.bufferedWriter()
            uriToWriter[normalized] = writer
            return writer
        }

        outlineFactoryService.generateOutlines(::provideOutput, nodes)

        uriToWriter.values.forEach { it.close() }
    }

    override fun buildSupportFiles() {}

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {

    }
}

fun DocumentationNode.signature() = detail(NodeKind.Signature).name
fun DocumentationNode.signatureUrlEncoded() = URLEncoder.encode(detail(NodeKind.Signature).name, "UTF-8")


fun URI.relativeTo(base: URI): URI {
    var base = base
    var child = this
    // Normalize paths to remove . and .. segments
    base = base.normalize()
    child = child.normalize()

    // Split paths into segments
    var bParts = base.path.split('/').dropLastWhile { it.isEmpty() }
    val cParts = child.path.split('/').dropLastWhile { it.isEmpty() }

    // Discard trailing segment of base path
    if (bParts.isNotEmpty() && !base.path.endsWith("/")) {
        bParts = bParts.dropLast(1)
    }

    // Remove common prefix segments
    var i = 0
    while (i < bParts.size && i < cParts.size && bParts[i] == cParts[i]) {
        i++
    }


    // Construct the relative path
    val sb = StringBuilder()
    for (j in 0 until bParts.size - i) {
        sb.append("../")
    }
    for (j in i until cParts.size) {
        if (j != i) {
            sb.append("/")
        }
        sb.append(cParts[j])
    }

    return URI.create(sb.toString())
}