package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import com.google.inject.name.Named
import kotlinx.html.*
import org.jetbrains.dokka.*
import java.io.File
import java.lang.Math.max
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
     */
    private fun Appendable.appendFamily(selectedNode: DocumentationNode, family: List<DocumentationNode>) {
        appendln("      <ul>")
        for (node in family) {
            val selected = if (node == selectedNode) "selected " else ""
            appendln("          <li class=\"${selected}api apilevel-${node.apiLevel.name}\">" +
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
            if (node.apiLevel.name != "") {
                div(classes = "api-level") {
                    +"added in "
                    a(href = "https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels") {
                        +"API level ${node.apiLevel.name}"
                    }
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

    override fun summary(node: DocumentationNode): ContentNode {
        return node.firstSentenceOfSummary()
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
                header = "Nested classes",
                summaryId = "nestedclasses",
                tableClass = "responsive",
                headerAsRow = true
        ) {
            nestedClassSummaryRow(it)
        }

        summaryNodeGroup(attributes, header="XML attributes", summaryId="lattrs", tableClass = "responsive", headerAsRow = true) { xmlAttributeRow(it) }

        expandableSummaryNodeGroupForInheritedMembers(
                superClasses = inheritedAttributes.entries,
                header="Inherited XML attributes",
                tableId="inhattrs",
                tableClass = "responsive"
        )

        summaryNodeGroup(
                constants,
                header = "Constants",
                summaryId = "constants",
                tableClass = "responsive constants",
                headerAsRow = true
        ) { propertyLikeSummaryRow(it) }

        expandableSummaryNodeGroupForInheritedMembers(
                superClasses = inheritedConstants.entries,
                header = "Inherited constants",
                tableId = "inhconstants",
                tableClass = "responsive constants inhtable"
        )

        constructors.forEach { (visibility, group) ->
            summaryNodeGroup(
                    group,
                    header = "${visibility.capitalize()} constructors",
                    summaryId = "${visibility.take(3)}ctors",
                    tableClass = "responsive",
                    headerAsRow = true
            ) {
                functionLikeSummaryRow(it)
            }
        }

        functions.forEach { (visibility, group) ->
            summaryNodeGroup(
                    group,
                    header = "${visibility.capitalize()} methods",
                    summaryId = "${visibility.take(3)}methods",
                    tableClass = "responsive",
                    headerAsRow = true
            ) {
                functionLikeSummaryRow(it)
            }
        }

        summaryNodeGroup(
                companionFunctions,
                header = "Companion functions",
                summaryId = "compmethods",
                tableClass = "responsive",
                headerAsRow = true
        ) {
            functionLikeSummaryRow(it)
        }

        expandableSummaryNodeGroupForInheritedMembers(
                superClasses = inheritedFunctionsByReceiver.entries,
                header = "Inherited functions",
                tableId = "inhmethods",
                tableClass = "responsive"
        )

        summaryNodeGroup(
                extensionFunctions.entries,
                header = "Extension functions",
                summaryId = "extmethods",
                tableClass = "responsive",
                headerAsRow = true
        ) {
            extensionRow(it) {
                functionLikeSummaryRow(it)
            }
        }
        summaryNodeGroup(
                inheritedExtensionFunctions.entries,
                header = "Inherited extension functions",
                summaryId = "inhextmethods",
                tableClass = "responsive",
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

        expandableSummaryNodeGroupForInheritedMembers(
                superClasses = inheritedPropertiesByReceiver.entries,
                header = "Inherited properties",
                tableId = "inhfields",
                tableClass = "responsive properties inhtable"
        )

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

    fun <T> FlowContent.summaryNodeGroup(
            nodes: Iterable<T>,
            header: String,
            headerAsRow: Boolean,
            summaryId: String,
            tableClass: String,
            row: TBODY.(T) -> Unit
    ) {
        if (nodes.none()) return
        if (!headerAsRow) {
            h2 { +header }
        }
        table(classes = "responsive") {
            id = summaryId
            tbody {
            if (headerAsRow)
                tr {
                    th {
                        attributes["colSpan"] = "2"
                        +header
                    }
                }
                nodes.forEach { node ->
                    row(node)
                }
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

    private fun FlowContent.expandableSummaryNodeGroupForInheritedMembers(
            tableId: String,
            header: String,
            tableClass: String,
            superClasses: Set<Map.Entry<DocumentationNode, List<DocumentationNode>>>
    ) {
        if (superClasses.none()) return
        table(classes = tableClass) {
            attributes["id"] = tableId
            tbody {
                tr {
                    th {
                        +header
                    }
                }
                superClasses.forEach { (superClass, members) ->
                    tr(classes = "api apilevel-${superClass.apiLevel}") {
                        td {
                            attributes["colSpan"] = "2"
                            div(classes = "expandable jd-inherited-apis") {
                                span(classes = "expand-control exw-expanded") {
                                    +"From class "
                                    code {
                                        a(href = superClass) { +superClass.name }
                                    }
                                }
                                table(classes = "responsive exw-expanded-content") {
                                    tbody {
                                        members.forEach { inheritedMember ->
                                            tr(classes = "api apilevel-${inheritedMember.apiLevel}") {
                                                attributes["data-version-added"] = "${inheritedMember.apiLevel}"
                                                td {
                                                    code {
                                                        renderedSignature(inheritedMember.detail(NodeKind.Type), LanguageService.RenderMode.SUMMARY)
                                                    }
                                                }
                                                td {
                                                    attributes["width"] = "100%"
                                                    code {
                                                        a(href = inheritedMember) { +inheritedMember.name }
                                                        if (inheritedMember.kind == NodeKind.Function) {
                                                            shortFunctionParametersList(inheritedMember)
                                                        }
                                                    }
                                                    p {
                                                        contentNodeToMarkup(inheritedMember.content)
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
            }
        }

    }

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


    override fun generatePackageIndex(page: Page.PackageIndex) = templateService.composePage(
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
                            }
                        }
                    }
                }
            }
    )

    override fun generateClassIndex(page: Page.ClassIndex) = templateService.composePage(
            page,
            htmlConsumer,
            headContent = {

            },
            bodyContent = {
                h1 { +"Class Index" }

                p {
                    +"These are all the API classes. See all "
                    a(href="https://developer.android.com/reference/packages.html") {
                        +"API packages."
                    }
                }

                div(classes = "jd-letterlist") {
                    page.classesByFirstLetter.forEach { (letter) ->
                        +"\n        "
                        a(href = "#letter_$letter") { +letter }
                        unsafe {
                            raw("&nbsp;&nbsp;")
                        }
                    }
                    +"\n    "
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

    override fun FlowContent.classHierarchy(superclasses: List<DocumentationNode>) {
        table(classes = "jd-inheritance-table") {
            var level = superclasses.size
            superclasses.forEach {
                tr {
                    var spaceColumns = max(superclasses.size - 1 - level, 0)
                    while (spaceColumns > 0) {
                        td(classes = "jd-inheritance-space") {
                            +" "
                        }
                        spaceColumns--
                    }
                    if (it != superclasses.first()) {
                        td(classes = "jd-inheritance-space") {
                            +"   ↳"
                        }
                    }
                    td(classes = "jd-inheritance-class-cell") {
                        attributes["colSpan"] = "$level"
                        qualifiedTypeReference(it)
                    }
                }
                level--
            }
        }
    }

    override fun FlowContent.subclasses(inheritors: List<DocumentationNode>, direct: Boolean) {
        if (inheritors.isEmpty()) return
        table(classes = "jd-sumtable jd-sumtable-subclasses") {
            tbody {
                tr {
                    td {
                        div(classes = "expandable") {
                            span(classes = "expand-control") {
                                if (direct)
                                    +"Known Direct Subclasses"
                                else
                                    +"Known Indirect Subclasses"
                            }
                            div(classes = "showalways") {
                                attributes["id"] = if (direct) "subclasses-direct" else "subclasses-indirect"
                                inheritors.forEach { inheritor ->
                                    a(href = inheritor) { +inheritor.classNodeNameWithOuterClass() }
                                    if (inheritor != inheritors.last()) +", "
                                }
                            }
                            div(classes = "exw-expanded-content") {
                                attributes["id"] = if (direct) "subclasses-direct-summary" else "subclasses-indirect-summary"
                                table(classes = "jd-sumtable-expando") {
                                    inheritors.forEach { inheritor ->
                                        tr(classes = "api api-level-${inheritor.apiLevel.name}") {
                                            attributes["data-version-added"] = inheritor.apiLevel.name
                                            td(classes = "jd-linkcol") {
                                                a(href = inheritor) { +inheritor.classNodeNameWithOuterClass() }
                                            }
                                            td(classes = "jd-descrcol") {
                                                attributes["width"] = "100%"
                                                contentNodeToMarkup(inheritor.firstSentenceOfSummary())
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
    }


    fun DocumentationNode.firstSentenceOfSummary(): ContentNode {

        fun Sequence<ContentNode>.flatten(): Sequence<ContentNode> {
            return flatMap {
                when (it) {
                    is ContentParagraph -> it.children.asSequence().flatten()
                    else -> sequenceOf(it)
                }
            }
        }

        fun ContentNode.firstSentence(): ContentText? = when(this) {
            is ContentText -> ContentText(text.takeWhile { it != '.' } + ".")
            else -> null
        }

        val elements = sequenceOf(summary).flatten()
        fun containsDot(it: ContentNode) = (it as? ContentText)?.text?.contains(".") == true

        val paragraph = ContentParagraph()
        (elements.takeWhile { !containsDot(it) } + elements.firstOrNull { containsDot(it) }?.firstSentence()).forEach {
            if (it != null) {
                paragraph.append(it)
            }
        }
        if (paragraph.isEmpty()) {
            return ContentEmpty
        }

        return paragraph
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