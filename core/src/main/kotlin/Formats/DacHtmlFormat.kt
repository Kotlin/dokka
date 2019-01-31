package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import com.google.inject.name.Named
import kotlinx.html.*
import org.jetbrains.dokka.*
import org.jetbrains.dokka.utilities.firstSentence
import java.lang.Math.max
import java.net.URI
import kotlin.reflect.KClass

/**
 * On Devsite, certain headers and footers are needed for generating Devsite metadata.
 */
class DevsiteHtmlTemplateService @Inject constructor(
    @Named("outlineRoot") val outlineRoot: String,
    @Named("dacRoot") val dacRoot: String
) : JavaLayoutHtmlTemplateService {
    override fun composePage(page: JavaLayoutHtmlFormatOutputBuilder.Page, tagConsumer: TagConsumer<Appendable>, headContent: HEAD.() -> Unit, bodyContent: BODY.() -> Unit) {
        tagConsumer.html {
            attributes["devsite"] = "true"
            head {
                headContent()
                title {
                    +when (page) {
                        is JavaLayoutHtmlFormatOutputBuilder.Page.ClassIndex -> "Class Index"
                        is JavaLayoutHtmlFormatOutputBuilder.Page.ClassPage -> page.node.nameWithOuterClass()
                        is JavaLayoutHtmlFormatOutputBuilder.Page.PackageIndex -> "Package Index"
                        is JavaLayoutHtmlFormatOutputBuilder.Page.PackagePage -> page.node.nameWithOuterClass()
                    }
                }
                unsafe { +"{% setvar book_path %}${dacRoot}/${outlineRoot}_book.yaml{% endsetvar %}\n{% include \"_shared/_reference-head-tags.html\" %}\n" }
            }
            body {
                bodyContent()
            }
        }
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
        fullMemberDocs(node, node)
    }

    override fun FlowContent.fullMemberDocs(node: DocumentationNode, uriNode: DocumentationNode) {
        a {
            attributes["name"] = uriNode.signatureForAnchor(logger).anchorEncoded()
        }
        div(classes = "api apilevel-${node.apiLevel.name}") {
            attributes["data-version-added"] = node.apiLevel.name
            h3(classes = "api-name") {
                //id = node.signatureForAnchor(logger).urlEncoded()
                +node.name
            }
            if (node.apiLevel.name.isNotEmpty()) {
                div(classes = "api-level") {
                    +"added in "
                    a(href = "https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels") {
                        +"API level ${node.apiLevel.name}"
                    }
                }
            }
            pre(classes = "api-signature no-pretty-print") { renderedSignature(node, LanguageService.RenderMode.FULL) }
            deprecationWarningToMarkup(node, prefix = true)
            nodeContent(node, uriNode)
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
        val deprecation = formatDeprecationOrNull(node)
        return when (deprecation) {
            null -> node.firstSentenceOfSummary()
            else -> deprecation
        }
    }

    fun TBODY.xmlAttributeRow(attr: DocumentationNode) = tr {
        td {
            a(href = attr) {
                code {
                    +attr.attributeRef!!.name
                }
            }
        }
        td {
            nodeSummary(attr.attributeRef!!, attr)
        }
    }

    protected fun FlowContent.fullAttributeDocs(
        attributes: List<DocumentationNode>,
        header: String
    ) {
        if (attributes.none()) return
        h2 {
            +header
        }
        attributes.forEach {
            fullMemberDocs(it.attributeRef!!, it)
        }
    }

    override fun FlowContent.classLikeFullMemberDocs(page: Page.ClassPage) = with(page) {
        fullAttributeDocs(attributes, "XML attributes")
        fullMemberDocs(enumValues, "Enum values")
        fullMemberDocs(constants, "Constants")

        constructors.forEach { (visibility, group) ->
            fullMemberDocs(group, "${visibility.capitalize()} constructors")
        }

        functions.forEach { (visibility, group) ->
            fullMemberDocs(group, "${visibility.capitalize()} methods")
        }

        fullMemberDocs(properties, "Properties")

        fields.forEach { (visibility, group) ->
            fullMemberDocs(group, "${visibility.capitalize()} fields")
        }
        if (!hasMeaningfulCompanion) {
            fullMemberDocs(companionFunctions, "Companion functions")
            fullMemberDocs(companionProperties, "Companion properties")
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

        summaryNodeGroup(
            attributes,
            header="XML attributes",
            summaryId="lattrs",
            tableClass = "responsive",
            headerAsRow = true
        ) {
            xmlAttributeRow(it)
        }

        expandableSummaryNodeGroupForInheritedMembers(
                superClasses = inheritedAttributes.entries,
                header="Inherited XML attributes",
                tableId="inhattrs",
                tableClass = "responsive",
                row = { inheritedXmlAttributeRow(it)}
        )

        summaryNodeGroup(
                constants,
                header = "Constants",
                summaryId = "constants",
                tableClass = "responsive",
                headerAsRow = true
        ) { propertyLikeSummaryRow(it) }

        expandableSummaryNodeGroupForInheritedMembers(
                superClasses = inheritedConstants.entries,
                header = "Inherited constants",
                tableId = "inhconstants",
                tableClass = "responsive constants inhtable",
                row = { inheritedMemberRow(it) }
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
                tableClass = "responsive",
                row = { inheritedMemberRow(it) }
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

        fields.forEach { (visibility, group) ->
            summaryNodeGroup(
                group,
                header = "${visibility.capitalize()} fields",
                summaryId = "${visibility.take(3)}fields",
                tableClass = "responsive",
                headerAsRow = true
            ) { propertyLikeSummaryRow(it) }
        }

        expandableSummaryNodeGroupForInheritedMembers(
            superClasses = inheritedFieldsByReceiver.entries,
            header = "Inherited fields",
            tableId = "inhfields",
            tableClass = "responsive properties inhtable",
            row = { inheritedMemberRow(it) }
        )

        summaryNodeGroup(
                properties,
                header = "Properties",
                summaryId = "properties",
                tableClass = "responsive",
                headerAsRow = true
        ) { propertyLikeSummaryRow(it) }


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
                tableClass = "responsive properties inhtable",
                row = { inheritedMemberRow(it) }
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
            tableClass: String = "responsive",
            row: TBODY.(T) -> Unit
    ) {
        if (nodes.none()) return
        if (!headerAsRow) {
            h2 { +header }
        }
        table(classes = tableClass) {
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
                nodeContent(page.node)
                summaryNodeGroup(page.classes.sortedBy { it.nameWithOuterClass().toLowerCase() }, "Classes", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.exceptions.sortedBy { it.nameWithOuterClass().toLowerCase() }, "Exceptions", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.typeAliases.sortedBy { it.nameWithOuterClass().toLowerCase() }, "Type-aliases", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.annotations.sortedBy { it.nameWithOuterClass().toLowerCase() }, "Annotations", headerAsRow = false) { classLikeRow(it) }
                summaryNodeGroup(page.enums.sortedBy { it.nameWithOuterClass().toLowerCase() }, "Enums", headerAsRow = false) { classLikeRow(it) }

                summaryNodeGroup(
                        page.constants.sortedBy { it.name },
                        "Top-level constants summary",
                        headerAsRow = false
                ) {
                    propertyLikeSummaryRow(it)
                }

                summaryNodeGroup(
                        page.functions.sortedBy { it.name },
                        "Top-level functions summary",
                        headerAsRow = false
                ) {
                    functionLikeSummaryRow(it)
                }

                summaryNodeGroup(
                        page.properties.sortedBy { it.name },
                        "Top-level properties summary",
                        headerAsRow = false
                ) {
                    propertyLikeSummaryRow(it)
                }

                summaryNodeGroupForExtensions("Extension functions summary", page.extensionFunctions.entries)
                summaryNodeGroupForExtensions("Extension properties summary", page.extensionProperties.entries)

                fullMemberDocs(page.constants.sortedBy { it.name }, "Top-level constants")
                fullMemberDocs(page.functions.sortedBy { it.name }, "Top-level functions")
                fullMemberDocs(page.properties.sortedBy { it.name }, "Top-level properties")
                fullMemberDocs(page.extensionFunctions.values.flatten().sortedBy { it.name }, "Extension functions")
                fullMemberDocs(page.extensionProperties.values.flatten().sortedBy { it.name }, "Extension properties")
            }
    )

    private fun TBODY.inheritedXmlAttributeRow(inheritedMember: DocumentationNode) {
        tr(classes = "api apilevel-${inheritedMember.attributeRef!!.apiLevel.name}") {
            attributes["data-version-added"] = "${inheritedMember.apiLevel}"
            td {
                code {
                    a(href = inheritedMember) { +inheritedMember.attributeRef!!.name }
                }
            }
            td {
                attributes["width"] = "100%"
                p {
                    nodeContent(inheritedMember.attributeRef!!, inheritedMember)
                }
            }
        }
    }

    private fun TBODY.inheritedMemberRow(inheritedMember: DocumentationNode) {
        tr(classes = "api apilevel-${inheritedMember.apiLevel.name}") {
            attributes["data-version-added"] = "${inheritedMember.apiLevel}"
            val type = inheritedMember.detailOrNull(NodeKind.Type)
            td {
                code {
                    type?.let {
                        renderedSignature(it, LanguageService.RenderMode.SUMMARY)
                    }
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
                    nodeContent(inheritedMember)
                }
            }
        }
    }

    private fun FlowContent.expandableSummaryNodeGroupForInheritedMembers(
            tableId: String,
            header: String,
            tableClass: String,
            superClasses: Set<Map.Entry<DocumentationNode, List<DocumentationNode>>>,
            row: TBODY.(inheritedMember: DocumentationNode) -> Unit
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
                    tr(classes = "api apilevel-${superClass.apiLevel.name}") {
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
                                            row(inheritedMember)
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

                                nodeSummary(node)
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
                    a(href="packages.html") {
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
                                        if (!deprecationWarningToMarkup(node)) {
                                            nodeSummary(node)
                                        }
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

        // The number of subclasses in collapsed view before truncating and adding a "and xx others".
        // See https://developer.android.com/reference/android/view/View for an example.
        val numToShow = 12

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

                                inheritors.take(numToShow).forEach { inheritor ->
                                    a(href = inheritor) { +inheritor.classNodeNameWithOuterClass() }
                                    if (inheritor != inheritors.last()) +", "
                                }

                                if (inheritors.size > numToShow) {
                                    +"and ${inheritors.size - numToShow} others."
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
                                                nodeSummary(inheritor)
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
            is ContentText -> ContentText(text.firstSentence())
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
