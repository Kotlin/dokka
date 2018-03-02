package org.jetbrains.dokka.Formats

import kotlinx.html.*
import kotlinx.html.Entities.nbsp
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.*
import org.jetbrains.dokka.LanguageService.RenderMode.FULL
import org.jetbrains.dokka.LanguageService.RenderMode.SUMMARY
import org.jetbrains.dokka.NodeKind.Companion.classLike
import java.net.URI
import javax.inject.Inject


open class JavaLayoutHtmlFormatOutputBuilder(
    val output: Appendable,
    val languageService: LanguageService,
    val uriProvider: JavaLayoutHtmlUriProvider,
    val templateService: JavaLayoutHtmlTemplateService,
    val logger: DokkaLogger,
    val uri: URI
) {

    val htmlConsumer = output.appendHTML()


    private fun FlowContent.hN(
        level: Int,
        classes: String? = null,
        block: CommonAttributeGroupFacadeFlowHeadingPhrasingContent.() -> Unit
    ) {
        when (level) {
            1 -> h1(classes, block)
            2 -> h2(classes, block)
            3 -> h3(classes, block)
            4 -> h4(classes, block)
            5 -> h5(classes, block)
            6 -> h6(classes, block)
        }
    }

    protected open fun FlowContent.metaMarkup(content: List<ContentNode>) = contentNodesToMarkup(content)
    protected open fun FlowContent.metaMarkup(content: ContentNode) = contentNodeToMarkup(content)

    private fun FlowContent.contentNodesToMarkup(content: List<ContentNode>): Unit = content.forEach { contentNodeToMarkup(it) }
    protected fun FlowContent.contentNodeToMarkup(content: ContentNode) {
        when (content) {
            is ContentText -> +content.text
            is ContentSymbol -> span("symbol") { +content.text }
            is ContentKeyword -> span("keyword") { +content.text }
            is ContentIdentifier -> span("identifier") {
                content.signature?.let { id = it }
                +content.text
            }

            is ContentHeading -> hN(level = content.level) { contentNodesToMarkup(content.children) }

            is ContentEntity -> +content.text

            is ContentStrong -> strong { contentNodesToMarkup(content.children) }
            is ContentStrikethrough -> del { contentNodesToMarkup(content.children) }
            is ContentEmphasis -> em { contentNodesToMarkup(content.children) }

            is ContentOrderedList -> ol { contentNodesToMarkup(content.children) }
            is ContentUnorderedList -> ul { contentNodesToMarkup(content.children) }
            is ContentListItem -> consumer.li {
                (content.children.singleOrNull() as? ContentParagraph)
                    ?.let { paragraph -> contentNodesToMarkup(paragraph.children) }
                        ?: contentNodesToMarkup(content.children)
            }


            is ContentCode -> contentInlineCode(content)
            is ContentBlockSampleCode -> contentBlockSampleCode(content)
            is ContentBlockCode -> contentBlockCode(content)


            ContentNonBreakingSpace -> +nbsp
            ContentSoftLineBreak, ContentIndentedSoftLineBreak -> {}
            ContentHardLineBreak -> br

            is ContentParagraph -> p { contentNodesToMarkup(content.children) }

            is ContentNodeLink -> {
                fun FlowContent.body() = contentNodesToMarkup(content.children)

                when (content.node?.kind) {
                    NodeKind.TypeParameter -> body()
                    else -> a(href = content.node, block = FlowContent::body)
                }
            }
            is ContentExternalLink -> contentExternalLink(content)
            is ContentSection -> {}
            is ContentBlock -> contentNodesToMarkup(content.children)
        }
    }

    protected open fun FlowContent.contentInlineCode(content: ContentCode) {
        code { contentNodesToMarkup(content.children) }
    }

    protected open fun FlowContent.contentBlockSampleCode(content: ContentBlockSampleCode) {
        pre {
            code {
                attributes["data-language"] = content.language
                contentNodesToMarkup(content.importsBlock.children)
                +"\n\n"
                contentNodesToMarkup(content.children)
            }
        }
    }

    protected open fun FlowContent.contentBlockCode(content: ContentBlockCode) {
        pre {
            code {
                attributes["data-language"] = content.language
                contentNodesToMarkup(content.children)
            }
        }
    }

    protected open fun FlowContent.contentExternalLink(content: ContentExternalLink) {
        a(href = content.href) { contentNodesToMarkup(content.children) }
    }

    protected open fun <T> FlowContent.summaryNodeGroup(
        nodes: Iterable<T>,
        header: String,
        headerAsRow: Boolean = true,
        row: TBODY.(T) -> Unit
    ) {
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


    protected open fun TBODY.classLikeRow(node: DocumentationNode) = tr {
        td { a(href = uriProvider.linkTo(node, uri)) { +node.simpleName() } }
        td { contentNodeToMarkup(node.summary) }
    }

    protected fun FlowContent.modifiers(node: DocumentationNode) {
        for (modifier in node.details(NodeKind.Modifier)) {
            renderedSignature(modifier, SUMMARY)
        }
    }

    protected fun FlowContent.shortFunctionParametersList(func: DocumentationNode) {
        val params = func.details(NodeKind.Parameter)
            .map { languageService.render(it, FULL) }
            .run {
                drop(1).fold(listOfNotNull(firstOrNull())) { acc, node ->
                    acc + ContentText(", ") + node
                }
            }
        metaMarkup(listOf(ContentText("(")) + params + listOf(ContentText(")")))
    }


    protected open fun TBODY.functionLikeSummaryRow(node: DocumentationNode) = tr {
        if (node.kind != NodeKind.Constructor) {
            td {
                modifiers(node)
                renderedSignature(node.detail(NodeKind.Type), SUMMARY)
            }
        }
        td {
            div {
                code {
                    val receiver = node.detailOrNull(NodeKind.Receiver)
                    if (receiver != null) {
                        renderedSignature(receiver.detail(NodeKind.Type), SUMMARY)
                        +"."
                    }
                    a(href = node) { +node.name }
                    shortFunctionParametersList(node)
                }
            }

            contentNodeToMarkup(node.summary)
        }
    }

    protected open fun TBODY.propertyLikeSummaryRow(node: DocumentationNode) = tr {
        td {
            modifiers(node)
            renderedSignature(node.detail(NodeKind.Type), SUMMARY)
        }
        td {
            div {
                code {
                    a(href = node) { +node.name }
                }
            }

            contentNodeToMarkup(node.summary)
        }
    }

    protected open fun TBODY.nestedClassSummaryRow(node: DocumentationNode) = tr {
        td {
            modifiers(node)
        }
        td {
            div {
                code {
                    a(href = node) { +node.name }
                }
            }

            contentNodeToMarkup(node.summary)
        }
    }

    protected open fun TBODY.inheritRow(
        entry: Map.Entry<DocumentationNode, List<DocumentationNode>>,
        summaryRow: TBODY.(DocumentationNode) -> Unit
    ) = tr {
        td {
            val (from, nodes) = entry
            +"From class "
            a(href = from.owner!!) { +from.qualifiedName() }
            table {
                tbody {
                    for (node in nodes) {
                        summaryRow(node)
                    }
                }
            }
        }
    }

    protected open fun TBODY.extensionRow(
        entry: Map.Entry<DocumentationNode, List<DocumentationNode>>,
        summaryRow: TBODY.(DocumentationNode) -> Unit
    ) = tr {
        td {
            val (from, nodes) = entry
            +"From "
            a(href = from) { +from.qualifiedName() }
            table {
                tbody {
                    for (node in nodes) {
                        summaryRow(node)
                    }
                }
            }
        }
    }

    protected open fun FlowContent.a(href: DocumentationNode?, classes: String? = null, block: A.() -> Unit) {
        if (href == null) {
            return a(href = "#", classes = classes, block = block)
        }

        val hrefText =
            href.name.takeIf { href.kind == NodeKind.ExternalLink }
                    ?: href.links.firstOrNull { it.kind == NodeKind.ExternalLink }?.name
                    ?: "#".takeIf { href.kind == NodeKind.ExternalClass } // When external class unresolved
                    ?: uriProvider.linkTo(href, uri)

        a(href = hrefText, classes = classes, block = block)
    }

    protected open fun FlowContent.renderedSignature(node: DocumentationNode, mode: LanguageService.RenderMode = SUMMARY) {
        contentNodeToMarkup(languageService.render(node, mode))
    }

    protected open fun generatePackage(page: Page.PackagePage) = templateService.composePage(
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

            fullMemberDocs(page.constants, "Top-level constants")
            fullMemberDocs(page.functions, "Top-level functions")
            fullMemberDocs(page.properties, "Top-level properties")
        }
    )

    protected fun FlowContent.qualifiedTypeReference(node: DocumentationNode) {
        if (node.kind in classLike) {
            a(href = node) { +node.qualifiedName() }
            return
        }

        val targetLink = node.links.singleOrNull()

        if (targetLink?.kind == NodeKind.TypeParameter) {
            +node.name
            return
        }

        a(href = targetLink) {
            +node.qualifiedNameFromType()
        }
        val typeParameters = node.details(NodeKind.Type)
        if (typeParameters.isNotEmpty()) {
            +"<"
            typeParameters.forEach {
                if (it != typeParameters.first()) {
                    +", "
                }
                qualifiedTypeReference(it)
            }
            +">"
        }
    }

    protected open fun FlowContent.classHierarchy(superclasses: List<DocumentationNode>) {
        table {
            superclasses.forEach {
                tr {
                    if (it != superclasses.first()) {
                        td {
                            +"   ↳"
                        }
                    }
                    td {
                        qualifiedTypeReference(it)
                    }
                }
            }
        }
    }

    protected open fun FlowContent.subclasses(inheritors: List<DocumentationNode>, direct: Boolean) {
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
                    inheritors.forEach { inheritor ->
                        tr {
                            td {
                                a(href = inheritor) { +inheritor.classNodeNameWithOuterClass() }
                            }
                            td {
                                contentNodeToMarkup(inheritor.summary)
                            }
                        }
                    }
                }
            }
        }
    }

    protected open fun FlowContent.classLikeSummaries(page: Page.ClassPage) = with(page) {
        summaryNodeGroup(
            nestedClasses,
            "Nested classes",
            headerAsRow = true
        ) {
            nestedClassSummaryRow(it)
        }

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

    protected open fun FlowContent.classLikeFullMemberDocs(page: Page.ClassPage) = with(page) {
        fullMemberDocs(constants, "Constants")
        fullMemberDocs(constructors, "Constructors")
        fullMemberDocs(functions, "Functions")
        fullMemberDocs(properties, "Properties")
        if (!hasMeaningfulCompanion) {
            fullMemberDocs(companionFunctions, "Companion functions")
            fullMemberDocs(companionProperties, "Companion properties")
        }
    }

    protected open fun generateClassLike(page: Page.ClassPage) = templateService.composePage(
        page,
        htmlConsumer,
        headContent = {

        },
        bodyContent = {
            val node = page.node
            with(page) {
                h1 { +node.name }
                pre { renderedSignature(node, FULL) }
                classHierarchy(page.superclasses)

                subclasses(page.directInheritors, true)
                subclasses(page.indirectInheritors, false)

                contentNodeToMarkup(node.content)

                h2 { +"Summary" }
                classLikeSummaries(page)

                classLikeFullMemberDocs(page)
            }
        }
    )

    protected open fun generateClassIndex(page: Page.ClassIndex) = templateService.composePage(
        page,
        htmlConsumer,
        headContent = {

        },
        bodyContent = {
            h1 { +"Class Index" }


            ul {
                page.classesByFirstLetter.forEach { (letter) ->
                    li { a(href = "#letter_$letter") { +letter } }
                }
            }

            page.classesByFirstLetter.forEach { (letter, classes) ->
                h2 {
                    id = "letter_$letter"
                    +letter
                }
                table {
                    tbody {
                        for (node in classes) {
                            tr {
                                td {
                                    a(href = uriProvider.linkTo(node, uri)) { +node.classNodeNameWithOuterClass() }
                                }
                                td {
                                    contentNodeToMarkup(node.content)
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    protected open fun generatePackageIndex(page: Page.PackageIndex) = templateService.composePage(
        page,
        htmlConsumer,
        headContent = {

        },
        bodyContent = {
            h1 { +"Package Index" }
            table {
                tbody {
                    for (node in page.packages) {
                        tr {
                            td {
                                a(href = uriProvider.linkTo(node, uri)) { +node.name }
                            }
                            td {
                                contentNodeToMarkup(node.content)
                            }
                        }
                    }
                }
            }
        }
    )

    fun generatePage(page: Page) {
        when (page) {
            is Page.PackageIndex -> generatePackageIndex(page)
            is Page.ClassIndex -> generateClassIndex(page)
            is Page.ClassPage -> generateClassLike(page)
            is Page.PackagePage -> generatePackage(page)
        }
    }

    protected fun FlowContent.fullMemberDocs(
        nodes: List<DocumentationNode>,
        header: String
    ) {
        if (nodes.none()) return
        h2 {
            +header
        }
        for (node in nodes) {
            fullMemberDocs(node)
        }
    }

    protected open fun FlowContent.seeAlsoSection(links: List<List<ContentNode>>) {
        p { b { +"See Also" } }
        ul {
            links.forEach { linkParts ->
                li { code { metaMarkup(linkParts) } }
            }
        }
    }

    protected open fun FlowContent.regularSection(name: String, entries: List<ContentSection>) {
        table {
            thead {
                tr {
                    th {
                        colSpan = "2"
                        +name
                    }
                }
            }
            tbody {
                entries.forEach {
                    tr {
                        if (it.subjectName != null) {
                            td { +it.subjectName }
                        }
                        td {
                            metaMarkup(it.children)
                        }
                    }
                }
            }
        }
    }

    protected open fun FlowContent.section(name: String, sectionParts: List<ContentSection>) {
        when(name) {
            ContentTags.SeeAlso -> seeAlsoSection(sectionParts.map { it.children.flatMap { (it as ContentParagraph).children } })
            else -> regularSection(name, sectionParts)
        }
    }

    protected open fun FlowContent.sections(content: Content) {
        val sectionsByTag = content.sections.groupByTo(mutableMapOf()) { it.tag }

        val seeAlso = sectionsByTag.remove(ContentTags.SeeAlso)

        for ((name, entries) in sectionsByTag) {
            section(name, entries)
        }

        seeAlso?.let { section(ContentTags.SeeAlso, it) }
    }

    protected open fun FlowContent.fullMemberDocs(node: DocumentationNode) {
        div {
            id = node.signatureForAnchor(logger)
            h3 { +node.name }
            pre { renderedSignature(node, FULL) }
            contentNodeToMarkup(node.content)
            node.constantValue()?.let { value ->
                pre {
                    +"Value: "
                    code { +value }
                }
            }

            sections(node.content)
        }
    }

    sealed class Page {
        class PackageIndex(packages: List<DocumentationNode>) : Page() {
            init {
                assert(packages.all { it.kind == NodeKind.Package })
            }

            val packages = packages.sortedBy { it.name }
        }

        class ClassIndex(allTypesNode: DocumentationNode) : Page() {
            init {
                assert(allTypesNode.kind == NodeKind.AllTypes)
            }

            // Wide-collect all nested classes
            val classes: List<DocumentationNode> =
                generateSequence(listOf(allTypesNode)) { nodes ->
                    nodes
                        .flatMap { it.members.filter { it.kind in NodeKind.classLike } }
                        .takeUnless { it.isEmpty() }
                }.drop(1)
                    .flatten()
                    .sortedBy { it.classNodeNameWithOuterClass() }
                    .toList()


            // Group all classes by it's first letter and sort
            val classesByFirstLetter =
                classes
                    .groupBy {
                        it.classNodeNameWithOuterClass().first().toString()
                    }
                    .entries
                    .sortedBy { (letter) -> letter }
        }

        class ClassPage(val node: DocumentationNode) : Page() {

            init {
                assert(node.kind in NodeKind.classLike)
            }

            val superclasses = (sequenceOf(node) + node.superclassTypeSequence).toList().asReversed()


            val directInheritors: List<DocumentationNode>
            val indirectInheritors: List<DocumentationNode>

            init {
                // Wide-collect all inheritors
                val inheritors = generateSequence(node.inheritors) { inheritors ->
                    inheritors
                        .flatMap { it.inheritors }
                        .takeUnless { it.isEmpty() }
                }
                directInheritors = inheritors.first()
                indirectInheritors = inheritors.drop(1).flatten().toList()
            }

            val isCompanion = node.details(NodeKind.Modifier).any { it.name == "companion" }
            val hasMeaningfulCompanion = !isCompanion && node.companion != null

            private fun DocumentationNode.thisTypeExtension() =
                detail(NodeKind.Receiver).detail(NodeKind.Type).links.any { it == node }

            val functionKind = if (!isCompanion) NodeKind.Function else NodeKind.CompanionObjectFunction
            val propertyKind = if (!isCompanion) NodeKind.Property else NodeKind.CompanionObjectProperty

            private fun DocumentationNode.isFunction() = kind == functionKind
            private fun DocumentationNode.isProperty() = kind == propertyKind


            val nestedClasses = node.members.filter { it.kind in NodeKind.classLike }

            val attributes = node.members(NodeKind.Attribute)

            val constants = node.members.filter { it.constantValue() != null }

            val constructors = node.members(NodeKind.Constructor)
            val functions = node.members(functionKind)
            val properties = node.members(propertyKind) - constants
            val inheritedFunctionsByReceiver = node.inheritedMembers(functionKind).groupBy { it.owner!! }
            val inheritedPropertiesByReceiver = node.inheritedMembers(propertyKind).groupBy { it.owner!! }

            val originalExtensions = if (!isCompanion) node.extensions else node.owner!!.extensions

            val extensionFunctions: Map<DocumentationNode, List<DocumentationNode>>
            val extensionProperties: Map<DocumentationNode, List<DocumentationNode>>
            val inheritedExtensionFunctions: Map<DocumentationNode, List<DocumentationNode>>
            val inheritedExtensionProperties: Map<DocumentationNode, List<DocumentationNode>>

            init {
                val (extensions, inheritedExtensions) = originalExtensions.partition { it.thisTypeExtension() }
                extensionFunctions = extensions.filter { it.isFunction() }.groupBy { it.owner!! }
                extensionProperties = extensions.filter { it.isProperty() }.groupBy { it.owner!! }
                inheritedExtensionFunctions =
                        inheritedExtensions.filter { it.isFunction() }.groupBy { it.owner!! }
                inheritedExtensionProperties =
                        inheritedExtensions.filter { it.isProperty() }.groupBy { it.owner!! }
            }

            val companionFunctions = node.members(NodeKind.CompanionObjectFunction).takeUnless { isCompanion }.orEmpty()
            val companionProperties =
                node.members(NodeKind.CompanionObjectProperty).takeUnless { isCompanion }.orEmpty() - constants


        }

        class PackagePage(val node: DocumentationNode) : Page() {

            init {
                assert(node.kind == NodeKind.Package)
            }

            val classes = node.members(NodeKind.Class)
            val exceptions = node.members(NodeKind.Exception)
            val typeAliases = node.members(NodeKind.TypeAlias)
            val annotations = node.members(NodeKind.AnnotationClass)
            val enums = node.members(NodeKind.Enum)

            private val externalClassExtensionFunctions =
                node.members(NodeKind.ExternalClass).flatMap { it.members(NodeKind.Function) }
            private val externalClassExtensionProperties =
                node.members(NodeKind.ExternalClass).flatMap { it.members(NodeKind.Property) }

            val constants = node.members(NodeKind.Property).filter { it.constantValue() != null }

            val functions = node.members(NodeKind.Function) + externalClassExtensionFunctions

            val properties = node.members(NodeKind.Property) - constants + externalClassExtensionProperties

        }
    }
}

class JavaLayoutHtmlFormatOutputBuilderFactoryImpl @Inject constructor(
    val uriProvider: JavaLayoutHtmlUriProvider,
    val languageService: LanguageService,
    val templateService: JavaLayoutHtmlTemplateService,
    val logger: DokkaLogger
) : JavaLayoutHtmlFormatOutputBuilderFactory {
    override fun createOutputBuilder(output: Appendable, node: DocumentationNode): JavaLayoutHtmlFormatOutputBuilder {
        return createOutputBuilder(output, uriProvider.mainUri(node))
    }

    override fun createOutputBuilder(output: Appendable, uri: URI): JavaLayoutHtmlFormatOutputBuilder {
        return JavaLayoutHtmlFormatOutputBuilder(output, languageService, uriProvider, templateService, logger, uri)
    }
}

fun DocumentationNode.constantValue(): String? =
    detailOrNull(NodeKind.Value)?.name.takeIf {
        kind == NodeKind.Property || kind == NodeKind.CompanionObjectProperty
    }