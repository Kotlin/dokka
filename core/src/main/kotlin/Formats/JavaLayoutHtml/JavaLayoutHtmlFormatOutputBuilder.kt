package org.jetbrains.dokka.Formats

import com.google.common.base.Throwables
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

    protected open fun FlowContent.metaMarkup(content: List<ContentNode>, contextUri: URI = uri) =
            contentNodesToMarkup(content, contextUri)

    protected fun FlowContent.nodeContent(node: DocumentationNode, uriNode: DocumentationNode) =
        contentNodeToMarkup(node.content, uriProvider.mainUriOrWarn(uriNode) ?: uri)

    protected fun FlowContent.nodeContent(node: DocumentationNode) =
        nodeContent(node, node)

    protected fun FlowContent.contentNodesToMarkup(content: List<ContentNode>, contextUri: URI = uri): Unit =
        content.forEach { contentNodeToMarkup(it, contextUri) }

    private fun FlowContent.contentNodeToMarkup(content: ContentNode, contextUri: URI) {
        when (content) {
            is ContentText -> +content.text
            is ContentSymbol -> span("symbol") { +content.text }
            is ContentKeyword -> span("keyword") { +content.text }
            is ContentIdentifier -> span("identifier") {
                content.signature?.let { id = it }
                +content.text
            }

            is ContentHeading -> hN(level = content.level) { contentNodesToMarkup(content.children, contextUri) }

            is ContentEntity -> +content.text

            is ContentStrong -> strong { contentNodesToMarkup(content.children, contextUri) }
            is ContentStrikethrough -> del { contentNodesToMarkup(content.children, contextUri) }
            is ContentEmphasis -> em { contentNodesToMarkup(content.children, contextUri) }

            is ContentOrderedList -> ol { contentNodesToMarkup(content.children, contextUri) }
            is ContentUnorderedList -> ul { contentNodesToMarkup(content.children, contextUri) }
            is ContentListItem -> consumer.li {
                (content.children.singleOrNull() as? ContentParagraph)
                        ?.let { paragraph -> contentNodesToMarkup(paragraph.children, contextUri) }
                        ?: contentNodesToMarkup(content.children, contextUri)
            }

            is ContentSpecialReference -> aside(classes = "note") {
                contentNodesToMarkup(content.children, contextUri)
            }

            is ContentCode -> contentInlineCode(content)
            is ContentBlockSampleCode -> contentBlockSampleCode(content)
            is ContentBlockCode -> contentBlockCode(content)

            ContentNonBreakingSpace -> +nbsp
            ContentSoftLineBreak, ContentIndentedSoftLineBreak -> {
            }
            ContentHardLineBreak -> br

            is ContentParagraph -> p { contentNodesToMarkup(content.children, contextUri) }

            is NodeRenderContent -> renderedSignature(content.node, mode = content.mode)
            is ContentNodeLink -> {
                fun FlowContent.body() = contentNodesToMarkup(content.children, contextUri)

                when (content.node?.kind) {
                    NodeKind.TypeParameter -> body()
                    else -> a(href = content.node, block = FlowContent::body)
                }
            }
            is ContentBookmark -> a {
                id = content.name
                contentNodesToMarkup(content.children, contextUri)
            }
            is ContentExternalLink -> contentExternalLink(content)
            is ContentLocalLink -> a(href = contextUri.resolve(content.href).relativeTo(uri).toString()) {
                contentNodesToMarkup(content.children, contextUri)
            }
            is ContentSection -> {
            }
            is ContentBlock -> contentNodesToMarkup(content.children, contextUri)
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


    protected open fun summary(node: DocumentationNode) = node.summary

    protected open fun TBODY.classLikeRow(node: DocumentationNode) = tr {
        td { a(href = uriProvider.linkTo(node, uri)) { +node.simpleName() } }
        td { nodeSummary(node) }
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

            nodeSummary(node)
        }
    }

    protected open fun TBODY.propertyLikeSummaryRow(node: DocumentationNode, showSignature: Boolean = true) = tr {
        if (showSignature) {
            td {
                modifiers(node)
                renderedSignature(node.detail(NodeKind.Type), SUMMARY)
            }
        }
        td {
            div {
                code {
                    a(href = node) { +node.name }
                }
            }

            nodeSummary(node)
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

            nodeSummary(node)
        }
    }

    protected fun HtmlBlockTag.nodeSummary(node: DocumentationNode, uriNode: DocumentationNode) {
        contentNodeToMarkup(summary(node), uriProvider.mainUriOrWarn(uriNode) ?: uri)
    }

    protected fun HtmlBlockTag.nodeSummary(node: DocumentationNode) {
        nodeSummary(node, node)
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

    protected open fun TBODY.groupedRow(
        entry: Map.Entry<DocumentationNode, List<DocumentationNode>>,
        groupHeader: HtmlBlockTag.(DocumentationNode) -> Unit,
        summaryRow: TBODY.(DocumentationNode) -> Unit
    ) = tr {
        td {
            val (from, nodes) = entry
            groupHeader(from)
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
    ) = groupedRow(entry, { from ->
        +"From "
        a(href = from) { +from.qualifiedName() }
    }, summaryRow)


    protected open fun TBODY.extensionByReceiverRow(
        entry: Map.Entry<DocumentationNode, List<DocumentationNode>>,
        summaryRow: TBODY.(DocumentationNode) -> Unit
    ) = groupedRow(entry, { from ->
        +"For "
        a(href = from) { +from.name }
    }, summaryRow)

    protected open fun FlowOrInteractiveOrPhrasingContent.a(href: DocumentationNode?, classes: String? = null, block: HtmlBlockInlineTag.() -> Unit) {
        if (href == null) {
            return a(href = "#", classes = classes, block = block)
        }

        val hrefText = try {
            href.name.takeIf { href.kind == NodeKind.ExternalLink }
                    ?: href.links.firstOrNull { it.kind == NodeKind.ExternalLink }?.name
                    ?: "#".takeIf { href.kind == NodeKind.ExternalClass } // When external class unresolved
                    ?: uriProvider.linkTo(href, uri)
        } catch (e: Exception) {
            val owners = generateSequence(href) { it.owner }.toList().reversed()
            logger.warn("Exception while resolving link to ${owners.joinToString(separator = " ")}\n"
                    + Throwables.getStackTraceAsString(e))
            "#"
        }

        a(href = hrefText, classes = classes, block = block)
    }

    protected open fun FlowContent.renderedSignature(
        node: DocumentationNode,
        mode: LanguageService.RenderMode = SUMMARY
    ) {
        contentNodeToMarkup(languageService.render(node, mode), uri)
    }

    protected open fun generatePackage(page: Page.PackagePage) = templateService.composePage(
        page,
        htmlConsumer,
        headContent = {

        },
        bodyContent = {
            h1 { +page.node.name }
            nodeContent(page.node)
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

            summaryNodeGroup(
                page.extensionFunctions.entries,
                "Extension functions summary",
                headerAsRow = false
            ) {
                extensionByReceiverRow(it) {
                    functionLikeSummaryRow(it)
                }
            }

            summaryNodeGroup(
                page.extensionProperties.entries,
                "Extension properties summary",
                headerAsRow = false
            ) {
                extensionByReceiverRow(it) {
                    functionLikeSummaryRow(it)
                }
            }

            fullMemberDocs(page.constants, "Top-level constants")
            fullMemberDocs(page.functions, "Top-level functions")
            fullMemberDocs(page.properties, "Top-level properties")
            fullMemberDocs(page.extensionFunctions.values.flatten(), "Extension functions")
            fullMemberDocs(page.extensionProperties.values.flatten(), "Extension properties")
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
                                nodeSummary(inheritor)
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

        summaryNodeGroup(enumValues, "Enum values") {
            propertyLikeSummaryRow(it)
        }

        summaryNodeGroup(constants, "Constants") { propertyLikeSummaryRow(it) }

        constructors.forEach { (visibility, group) ->
            summaryNodeGroup(
                    group,
                    "${visibility.capitalize()} constructors",
                    headerAsRow = true
            ) {
                functionLikeSummaryRow(it)
            }
        }

        functions.forEach { (visibility, group) ->
            summaryNodeGroup(
                    group,
                    "${visibility.capitalize()} functions",
                    headerAsRow = true
            ) {
                functionLikeSummaryRow(it)
            }
        }

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
        fullMemberDocs(enumValues, "Enum values")
        fullMemberDocs(constants, "Constants")

        constructors.forEach { (visibility, group) ->
            fullMemberDocs(group, "${visibility.capitalize()} constructors")
        }

        functions.forEach { (visibility, group) ->
            fullMemberDocs(group, "${visibility.capitalize()} methods")
        }

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
                if (node.artifactId.name.isNotEmpty()) {
                    div(classes = "api-level") { br { +"belongs to Maven artifact ${node.artifactId}" } }
                }
                h1 { +node.name }
                pre { renderedSignature(node, FULL) }
                classHierarchy(page.superclasses)

                subclasses(page.directInheritors, true)
                subclasses(page.indirectInheritors, false)

                deprecatedClassCallOut(node)
                nodeContent(node)

                h2 { +"Summary" }
                classLikeSummaries(page)
                classLikeFullMemberDocs(page)
            }
        }
    )

    protected open fun FlowContent.classIndexSummary(node: DocumentationNode) {
        nodeContent(node)
    }

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
                                    if (!deprecatedIndexSummary(node)) {
                                        classIndexSummary(node)
                                    }
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
                                nodeContent(node)
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

    protected open fun FlowContent.deprecationWarningToMarkup(
        node: DocumentationNode,
        prefix: Boolean = false,
        emphasis: Boolean = true
    ): Boolean {
        val deprecated = formatDeprecationOrNull(node, prefix, emphasis)
        deprecated?.let {
            contentNodeToMarkup(deprecated, uriProvider.mainUri(node))
            return true
        }
        return false
    }

    protected open fun FlowContent.deprecatedClassCallOut(node: DocumentationNode) {
        val deprecatedLevelExists = node.deprecatedLevel.name.isNotEmpty()
        if (deprecatedLevelExists) {
            hr { }
            aside(classes = "caution") {
                strong { +node.deprecatedLevelMessage() }
                deprecationWarningToMarkup(node, emphasis = false)
            }
        }
    }

    protected open fun FlowContent.deprecatedIndexSummary(node: DocumentationNode): Boolean {
        val deprecatedLevelExists = node.deprecatedLevel.name.isNotEmpty()
        if (deprecatedLevelExists) {
            val em = ContentEmphasis()
            em.append(ContentText(node.deprecatedLevelMessage()))
            em.append(ContentText(" "))
            for (child in node.deprecation?.content?.children ?: emptyList<ContentNode>()) {
                em.append(child)
            }
            contentNodeToMarkup(em, uriProvider.mainUri(node))
            return true
        }
        return false
    }

    protected open fun FlowContent.apiAndDeprecatedVersions(node: DocumentationNode) {
        val apiLevelExists = node.apiLevel.name.isNotEmpty()
        val deprecatedLevelExists = node.deprecatedLevel.name.isNotEmpty()
        if (apiLevelExists || deprecatedLevelExists) {
            div(classes = "api-level") {
                if (apiLevelExists) {
                    +"Added in "
                    a(href = "https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels") {
                        +"API level ${node.apiLevel.name}"
                    }
                    if (deprecatedLevelExists) {
                        br
                    }
                }
                if (deprecatedLevelExists) {
                    +"Deprecated in "
                    a(href = "https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels") {
                        +"API level ${node.deprecatedLevel.name}"
                    }
                }
            }
        }
    }

    protected open fun formatDeprecationOrNull(
        node: DocumentationNode,
        prefix: Boolean = false,
        emphasis: Boolean = true): ContentNode? {
        val deprecated = node.deprecation
        deprecated?.let {
            return ContentParagraph().apply {
                if (prefix) {
                    append(ContentStrong().apply { text(
                        if (deprecated.content.children.size == 0) "Deprecated."
                        else "Deprecated: "
                    ) })
                }
                val em = if (emphasis) ContentEmphasis() else ContentBlock()
                for (child in deprecated.content.children) {
                    em.append(child)
                }
                append(em)
            }
        }
        return null
    }

    protected open fun FlowContent.section(name: String, sectionParts: List<ContentSection>) {
        when (name) {
            ContentTags.SeeAlso -> seeAlsoSection(sectionParts.map { it.children.flatMap { (it as? ContentParagraph)?.children ?: listOf(it) } })
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

    protected open fun FlowContent.fullMemberDocs(node: DocumentationNode, uriNode: DocumentationNode) {
        div {
            id = node.signatureForAnchor(logger)
            h3 { +node.name }
            pre { renderedSignature(node, FULL) }
            deprecationWarningToMarkup(node, prefix = true)
            nodeContent(node)
            node.constantValue()?.let { value ->
                pre {
                    +"Value: "
                    code { +value }
                }
            }

            sections(node.content)
        }
    }

    protected open fun FlowContent.fullMemberDocs(node: DocumentationNode) {
        fullMemberDocs(node, node)
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
                    .sortedBy { it.classNodeNameWithOuterClass().toLowerCase() }
                    .toList()


            // Group all classes by it's first letter and sort
            val classesByFirstLetter =
                classes
                    .groupBy {
                        it.classNodeNameWithOuterClass().first().toString()
                    }
                    .entries
                    .sortedBy { (letter) ->
                        val x = letter.toLowerCase()
                        x
                    }
        }

        class ClassPage(val node: DocumentationNode) : Page() {

            init {
                assert(node.kind in NodeKind.classLike)
            }

            val superclasses = (sequenceOf(node) + node.superclassTypeSequence).toList().asReversed()

            val enumValues = node.members(NodeKind.EnumItem).sortedBy { it.name }

            val directInheritors: List<DocumentationNode>
            val indirectInheritors: List<DocumentationNode>

            init {
                // Wide-collect all inheritors
                val inheritors = generateSequence(node.inheritors) { inheritors ->
                    inheritors
                        .flatMap { it.inheritors }
                        .takeUnless { it.isEmpty() }
                }
                directInheritors = inheritors.first().sortedBy { it.classNodeNameWithOuterClass() }
                indirectInheritors = inheritors.drop(1).flatten().toList().sortedBy { it.classNodeNameWithOuterClass() }
            }

            val isCompanion = node.details(NodeKind.Modifier).any { it.name == "companion" }
            val hasMeaningfulCompanion = !isCompanion && node.companion != null

            private fun DocumentationNode.thisTypeExtension() =
                detail(NodeKind.Receiver).detail(NodeKind.Type).links.any { it == node }

            val functionKind = if (!isCompanion) NodeKind.Function else NodeKind.CompanionObjectFunction
            val propertyKind = if (!isCompanion) NodeKind.Property else NodeKind.CompanionObjectProperty

            private fun DocumentationNode.isFunction() = kind == functionKind
            private fun DocumentationNode.isProperty() = kind == propertyKind


            val nestedClasses = node.members.filter { it.kind in NodeKind.classLike } - enumValues

            val attributes = node.attributes

            val inheritedAttributes =
                    node.superclassTypeSequence
                            .toList()
                            .sortedBy { it.name }
                            .flatMap { it.typeDeclarationClass?.attributes.orEmpty() }
                            .distinctBy { it.attributeRef!!.name }
                            .groupBy { it.owner!! }

            val allInheritedMembers = node.allInheritedMembers
            val constants = node.members.filter { it.constantValue() != null }
            val inheritedConstants = allInheritedMembers.filter { it.constantValue() != null }.groupBy { it.owner!! }


            fun compareVisibilities(a: String, b: String): Int {
                return visibilityNames.indexOf(a) - visibilityNames.indexOf(b)
            }

            fun Collection<DocumentationNode>.groupByVisibility() =
                    groupBy { it.visibility() }.toSortedMap(Comparator { a, b -> compareVisibilities(a, b) })


            val constructors = node.members(NodeKind.Constructor).groupByVisibility()
            val functions = node.members(functionKind).groupByVisibility()
            val fields = (node.members(NodeKind.Field) - constants).groupByVisibility()
            val properties = node.members(propertyKind) - constants
            val inheritedFunctionsByReceiver = allInheritedMembers.filter { it.kind == functionKind }.groupBy { it.owner!! }
            val inheritedPropertiesByReceiver =
                allInheritedMembers.filter {
                    it.kind == propertyKind && it.constantValue() == null
                }.groupBy { it.owner!! }

            val inheritedFieldsByReceiver =
                allInheritedMembers.filter {
                    it == NodeKind.Field && it.constantValue() != null
                }.groupBy { it.owner!! }

            val originalExtensions = if (!isCompanion) node.extensions else node.owner!!.extensions

            val extensionFunctions: Map<DocumentationNode, List<DocumentationNode>>
            val extensionProperties: Map<DocumentationNode, List<DocumentationNode>>
            val inheritedExtensionFunctions: Map<DocumentationNode, List<DocumentationNode>>
            val inheritedExtensionProperties: Map<DocumentationNode, List<DocumentationNode>>

            init {
                val (extensions, inheritedExtensions) = originalExtensions.partition { it.thisTypeExtension() }
                extensionFunctions = extensions.filter { it.isFunction() }.sortedBy { it.name }.groupBy { it.owner!! }
                extensionProperties = extensions.filter { it.isProperty() }.sortedBy { it.name }.groupBy { it.owner!! }
                inheritedExtensionFunctions =
                        inheritedExtensions.filter { it.isFunction() }.sortedBy { it.name }.groupBy { it.owner!! }
                inheritedExtensionProperties =
                        inheritedExtensions.filter { it.isProperty() }.sortedBy { it.name }.groupBy { it.owner!! }
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

            val constants = node.members(NodeKind.Property).filter { it.constantValue() != null }


            private fun DocumentationNode.getClassExtensionReceiver() =
                detailOrNull(NodeKind.Receiver)?.detailOrNull(NodeKind.Type)?.takeIf {
                    it.links.any { it.kind == NodeKind.ExternalLink || it.kind in NodeKind.classLike }
                }

            private fun List<DocumentationNode>.groupedExtensions() =
                filter { it.getClassExtensionReceiver() != null }
                    .groupBy {
                        val receiverType = it.getClassExtensionReceiver()!!
                        receiverType.links(NodeKind.ExternalLink).firstOrNull()
                                ?: receiverType.links.first { it.kind in NodeKind.classLike}
                    }

            private fun List<DocumentationNode>.externalExtensions(kind: NodeKind) =
                associateBy({ it }, { it.members(kind) })
                    .filterNot { (_, values) -> values.isEmpty() }

            val extensionFunctions =
                node.members(NodeKind.ExternalClass).externalExtensions(NodeKind.Function) +
                        node.members(NodeKind.Function).groupedExtensions()

            val extensionProperties =
                node.members(NodeKind.ExternalClass).externalExtensions(NodeKind.Property) +
                        node.members(NodeKind.Property).groupedExtensions()

            val functions = node.members(NodeKind.Function) - extensionFunctions.values.flatten()
            val properties = node.members(NodeKind.Property) - constants - extensionProperties.values.flatten()

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
        kind == NodeKind.Field || kind == NodeKind.Property || kind == NodeKind.CompanionObjectProperty
    }


private val visibilityNames = setOf("public", "protected", "internal", "package-local", "private")

fun DocumentationNode.visibility(): String =
        details(NodeKind.Modifier).firstOrNull { it.name in visibilityNames }?.name ?: ""