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
import org.jetbrains.dokka.Utilities.lazyBind
import org.jetbrains.dokka.Utilities.toOptional
import org.jetbrains.dokka.Utilities.toType
import org.jetbrains.kotlin.preprocessor.mkdirsOrFail
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
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
        bind<JavaLayoutHtmlUriProvider>() toType generatorServiceClass
        lazyBind<JavaLayoutHtmlFormatOutlineFactoryService>() toOptional outlineFactoryClass
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
                head {
                    meta(charset = "UTF-8")
                    headContent()
                }
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

    private fun TBODY.summaryRow(node: DocumentationNode) = tr {
        if (node.kind != NodeKind.Constructor) {
            td {
                for (modifier in node.details(NodeKind.Modifier)) {
                    renderedSignature(modifier, SUMMARY)
                }
                renderedSignature(node.detail(NodeKind.Type), SUMMARY)
            }
        }
        td {
            div {
                code {
                    a(href = uriProvider.linkTo(node, uri)) { +node.name }
                    val params = node.details(NodeKind.Parameter)
                            .map { languageService.render(it, FULL) }
                            .run {
                                drop(1).fold(listOfNotNull(firstOrNull())) { acc, node ->
                                    acc + ContentText(", ") + node
                                }
                            }
                    metaMarkup(listOf(ContentText("(")) + params + listOf(ContentText(")")))
                }
            }

            metaMarkup(node.summary)
        }
    }

    private fun TBODY.inheritRow(entry: Map.Entry<DocumentationNode, List<DocumentationNode>>) = tr {
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

                summaryNodeGroup(node.members(NodeKind.Function), "Top-level functions summary") { summaryRow(it) }
                summaryNodeGroup(node.members(NodeKind.Property), "Top-level properties summary") { summaryRow(it) }


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

    fun appendClassLike(node: DocumentationNode) = templateService.composePage(
            listOf(node),
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +node.name }
                pre { renderedSignature(node, FULL) }
                classHierarchy(node)

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


                summaryNodeGroup(node.members(NodeKind.Constructor), "Constructors", headerAsRow = true) { summaryRow(it) }

                summaryNodeGroup(functionsToDisplay, "Functions", headerAsRow = true) { summaryRow(it) }
                summaryNodeGroup(inheritedFunctionsByReceiver.entries, "Inherited functions", headerAsRow = true) { inheritRow(it) }
                summaryNodeGroup(extensionFunctions, "Extension functions", headerAsRow = true) { summaryRow(it) }


                summaryNodeGroup(properties, "Properties", headerAsRow = true) { summaryRow(it) }
                summaryNodeGroup(inheritedPropertiesByReceiver.entries, "Inherited properties", headerAsRow = true) { inheritRow(it) }
                summaryNodeGroup(extensionProperties, "Extension properties", headerAsRow = true) { summaryRow(it) }

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
                val classesByFirstLetter = allTypesNode.members
                        .filterNot { it.kind == NodeKind.ExternalClass }
                        .groupBy {
                            it.name.first().toString()
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
                            for (node in nodes) {
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
                a(href = content.node?.let { uriProvider.linkTo(it, uri) } ?: "#unresolved") { appendContent(content.children) }
            }
            is ContentExternalLink -> {
                a(href = content.href) { appendContent(content.children) }
            }

            is ContentBlock -> appendContent(content.children)
        }
    }
}


interface JavaLayoutHtmlUriProvider {
    fun tryGetContainerUri(node: DocumentationNode): URI?
    fun tryGetMainUri(node: DocumentationNode): URI?
    fun containerUri(node: DocumentationNode): URI = tryGetContainerUri(node) ?: error("Unsupported ${node.kind}")
    fun mainUri(node: DocumentationNode): URI = tryGetMainUri(node) ?: error("Unsupported ${node.kind}")

    fun linkTo(to: DocumentationNode, from: URI): String {
        return mainUri(to).relativeTo(from).toString()
    }

    fun mainUriOrWarn(node: DocumentationNode): URI? = tryGetMainUri(node) ?: (null).also {
        AssertionError("Not implemented mainUri for ${node.kind}").printStackTrace()
    }
}

class JavaLayoutHtmlFormatGenerator @Inject constructor(
        @Named("outputDir") val root: File,
        val languageService: LanguageService,
        val templateService: JavaLayoutHtmlTemplateService,
        val logger: DokkaLogger
) : Generator, JavaLayoutHtmlUriProvider {

    @set:Inject(optional = true)
    var outlineFactoryService: JavaLayoutHtmlFormatOutlineFactoryService? = null

    fun createOutputBuilderForNode(node: DocumentationNode, output: Appendable)
            = JavaLayoutHtmlFormatOutputBuilder(output, languageService, this, templateService, logger, mainUri(node))

    override fun tryGetContainerUri(node: DocumentationNode): URI? {
        return when (node.kind) {
            NodeKind.Module -> URI("/").resolve(node.name + "/")
            NodeKind.Package -> tryGetContainerUri(node.owner!!)?.resolve(node.name.replace('.', '/') + '/')
            in classLike -> tryGetContainerUri(node.owner!!)?.resolve("${node.name}.html")
            else -> null
        }
    }

    override fun tryGetMainUri(node: DocumentationNode): URI? {
        return when (node.kind) {
            NodeKind.Package -> tryGetContainerUri(node)?.resolve("package-summary.html")
            in classLike -> tryGetContainerUri(node)?.resolve("#")
            in memberLike -> {
                val owner = if (node.owner?.kind != NodeKind.ExternalClass) node.owner else node.owner?.owner
                tryGetMainUri(owner!!)?.resolveInPage(node)
            }
            NodeKind.TypeParameter, NodeKind.Parameter -> node.path.asReversed().drop(1).firstNotNullResult(this::tryGetMainUri)?.resolveInPage(node)
            NodeKind.AllTypes -> tryGetContainerUri(node.owner!!)?.resolve("classes.html")
            else -> null
        }
    }

    fun URI.resolveInPage(node: DocumentationNode): URI = resolve("#${node.signatureUrlEncoded(logger)}")

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

    fun buildClassIndex(node: DocumentationNode, parentDir: File) {
        val file = parentDir.resolve("classes.html")
        file.bufferedWriter().use {
            createOutputBuilderForNode(node, it).generateClassesIndex(node)
        }
    }

    fun buildPackageIndex(nodes: List<DocumentationNode>, parentDir: File) {
        val file = parentDir.resolve("packages.html")
        file.bufferedWriter().use {
            JavaLayoutHtmlFormatOutputBuilder(it, languageService, this, templateService, logger, containerUri(nodes.first().owner!!).resolve("packages.html"))
                    .generatePackageIndex(nodes)
        }
    }

    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single()

        val moduleRoot = root.resolve(module.name)
        val packages = module.members.filter { it.kind == NodeKind.Package }
        packages.forEach { buildPackage(it, moduleRoot) }

        buildClassIndex(module.members.single { it.kind == NodeKind.AllTypes }, moduleRoot)
        buildPackageIndex(packages, moduleRoot)
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

        outlineFactoryService?.generateOutlines(::provideOutput, nodes)

        uriToWriter.values.forEach { it.close() }
    }

    override fun buildSupportFiles() {}

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {

    }
}

fun DocumentationNode.signatureForAnchor(logger: DokkaLogger): String = when (kind) {
    NodeKind.Function, NodeKind.Constructor -> buildString {
        detailOrNull(NodeKind.Receiver)?.let {
            append("(")
            append(it.detail(NodeKind.Type).qualifiedNameFromType())
            append(").")
        }
        append(name)
        details(NodeKind.Parameter).joinTo(this, prefix = "(", postfix = ")") { it.detail(NodeKind.Type).qualifiedNameFromType() }
    }
    NodeKind.Property ->
        "$name:${detail(NodeKind.Type).qualifiedNameFromType()}"
    NodeKind.TypeParameter, NodeKind.Parameter -> owner!!.signatureForAnchor(logger) + "/" + name
    else -> "Not implemented signatureForAnchor $this".also { logger.warn(it) }
}

fun DocumentationNode.signatureUrlEncoded(logger: DokkaLogger) = URLEncoder.encode(signatureForAnchor(logger), "UTF-8")


fun URI.relativeTo(uri: URI): URI {
    // Normalize paths to remove . and .. segments
    val base = uri.normalize()
    val child = this.normalize()

    fun StringBuilder.appendRelativePath() {
        // Split paths into segments
        var bParts = base.path.split('/').dropLastWhile { it.isEmpty() }
        val cParts = child.path.split('/').dropLastWhile { it.isEmpty() }

        // Discard trailing segment of base path
        if (bParts.isNotEmpty() && !base.path.endsWith("/")) {
            bParts = bParts.dropLast(1)
        }

        // Compute common prefix
        val commonPartsSize = bParts.zip(cParts).count { (basePart, childPart) -> basePart == childPart }
        bParts.drop(commonPartsSize).joinTo(this, separator = "") { "../" }
        cParts.drop(commonPartsSize).joinTo(this, separator = "/")
    }

    return URI.create(buildString {
        if (base.path != child.path) {
            appendRelativePath()
        }
        child.rawQuery?.let {
            append("?")
            append(it)
        }
        child.rawFragment?.let {
            append("#")
            append(it)
        }
    })
}