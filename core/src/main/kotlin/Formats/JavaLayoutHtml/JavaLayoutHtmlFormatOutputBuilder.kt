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

    fun FlowContent.metaMarkup(content: List<ContentNode>): Unit = content.forEach { metaMarkup(it) }
    fun FlowContent.metaMarkup(content: ContentNode) {
        when (content) {
            is ContentText -> +content.text
            is ContentSymbol -> span("symbol") { +content.text }
            is ContentKeyword -> span("keyword") { +content.text }
            is ContentIdentifier -> span("identifier") {
                content.signature?.let { id = it }
                +content.text
            }

            is ContentHeading -> hN(level = content.level) { metaMarkup(content.children) }

            is ContentEntity -> +content.text

            is ContentStrong -> strong { metaMarkup(content.children) }
            is ContentStrikethrough -> del { metaMarkup(content.children) }
            is ContentEmphasis -> em { metaMarkup(content.children) }

            is ContentOrderedList -> ol { metaMarkup(content.children) }
            is ContentUnorderedList -> ul { metaMarkup(content.children) }
            is ContentListItem -> consumer.li {
                (content.children.singleOrNull() as? ContentParagraph)
                    ?.let { paragraph -> metaMarkup(paragraph.children) }
                        ?: metaMarkup(content.children)
            }


            is ContentCode -> pre { code { metaMarkup(content.children) } }
            is ContentBlockSampleCode -> pre { code {} }
            is ContentBlockCode -> pre { code {} }


            is ContentNonBreakingSpace -> +nbsp
            is ContentSoftLineBreak, is ContentIndentedSoftLineBreak -> {
            }

            is ContentParagraph -> p { metaMarkup(content.children) }

            is ContentNodeLink -> {
                val href = content.node?.let { uriProvider.linkTo(it, uri) } ?: "#"
                a(href = href) { metaMarkup(content.children) }
            }
            is ContentExternalLink -> {
                a(href = content.href) { metaMarkup(content.children) }
            }

            is ContentBlock -> metaMarkup(content.children)
        }
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
        td { metaMarkup(node.summary) }
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

            metaMarkup(node.summary)
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

            metaMarkup(node.summary)
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

            metaMarkup(node.summary)
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

    protected open fun FlowContent.a(href: DocumentationNode, classes: String? = null, block: A.() -> Unit) {
        val hrefText = if (href.kind == NodeKind.ExternalLink)
            href.name
        else
            uriProvider.linkTo(href, uri)
        a(href = hrefText, classes = classes, block = block)
    }

    protected open fun FlowContent.renderedSignature(node: DocumentationNode, mode: LanguageService.RenderMode = SUMMARY) {
        metaMarkup(languageService.render(node, mode))
    }

    protected open fun generatePackage(page: Page.PackagePage) = templateService.composePage(
        page,
        htmlConsumer,
        headContent = {

        },
        bodyContent = {
            h1 { +page.node.name }
            metaMarkup(page.node.content)
            summaryNodeGroup(page.classes, "Classes", headerAsRow = false) { classLikeRow(it) }
            summaryNodeGroup(page.exceptions, "Exceptions", headerAsRow = false) { classLikeRow(it) }
            summaryNodeGroup(page.typeAliases, "Type-aliases", headerAsRow = false) { classLikeRow(it) }
            summaryNodeGroup(page.annotations, "Annotations", headerAsRow = false) { classLikeRow(it) }
            summaryNodeGroup(page.enums, "Enums", headerAsRow = false) { classLikeRow(it) }

            summaryNodeGroup(
                page.functions,
                "Top-level functions summary",
                headerAsRow = false
            ) { functionLikeSummaryRow(it) }
            summaryNodeGroup(
                page.properties,
                "Top-level properties summary",
                headerAsRow = false
            ) { propertyLikeSummaryRow(it) }


            fullMemberDocs(page.functions, "Top-level functions")
            fullMemberDocs(page.properties, "Top-level properties")
        }
    )

    protected fun FlowContent.qualifiedTypeReference(node: DocumentationNode) {
        if (node.kind in classLike) {
            a(href = node) { +node.qualifiedName() }
            return
        }

        val targetLink = node.links.single()

        if (targetLink.kind == NodeKind.TypeParameter) {
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
                                metaMarkup(inheritor.summary)
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

                metaMarkup(node.content)

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
                                    metaMarkup(node.content)
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
                                metaMarkup(node.content)
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

    protected open fun FlowContent.fullMemberDocs(node: DocumentationNode) {
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
            val constructors = node.members(NodeKind.Constructor)
            val functions = node.members(functionKind)
            val properties = node.members(propertyKind)
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
                node.members(NodeKind.CompanionObjectProperty).takeUnless { isCompanion }.orEmpty()


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

            val functions = node.members(NodeKind.Function)
            val properties = node.members(NodeKind.Property)
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