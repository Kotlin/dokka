package org.jetbrains.dokka

import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocTagValue
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import org.jetbrains.dokka.DocumentationNode.Kind
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

data class JavadocParseResult(val content: Content, val deprecatedContent: Content?) {
    companion object {
        val Empty = JavadocParseResult(Content.Empty, null)
    }
}

interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement): JavadocParseResult
}

class JavadocParser(private val refGraph: NodeReferenceGraph) : JavaDocumentationParser {
    override fun parseDocumentation(element: PsiNamedElement): JavadocParseResult {
        val docComment = (element as? PsiDocCommentOwner)?.docComment
        if (docComment == null) return JavadocParseResult.Empty
        val result = MutableContent()
        var deprecatedContent: Content? = null
        val para = ContentParagraph()
        result.append(para)
        para.convertJavadocElements(docComment.descriptionElements.dropWhile { it.text.trim().isEmpty() })
        docComment.tags.forEach { tag ->
            when(tag.name) {
                "see" -> result.convertSeeTag(tag)
                "deprecated" -> {
                    deprecatedContent = Content()
                    deprecatedContent!!.convertJavadocElements(tag.contentElements())
                }
                else -> {
                    val subjectName = tag.getSubjectName()
                    val section = result.addSection(javadocSectionDisplayName(tag.name), subjectName)

                    section.convertJavadocElements(tag.contentElements())
                }
            }
        }
        return JavadocParseResult(result, deprecatedContent)
    }

    private fun PsiDocTag.contentElements(): Iterable<PsiElement> {
        val tagValueElements = children
                .dropWhile { it.node?.elementType == JavaDocTokenType.DOC_TAG_NAME }
                .dropWhile { it is PsiWhiteSpace }
                .filterNot { it.node?.elementType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS }
        return if (getSubjectName() != null) tagValueElements.dropWhile { it is PsiDocTagValue } else tagValueElements
    }

    private fun ContentBlock.convertJavadocElements(elements: Iterable<PsiElement>) {
        val htmlBuilder = StringBuilder()
        elements.forEach {
            if (it is PsiInlineDocTag) {
                htmlBuilder.append(convertInlineDocTag(it))
            } else {
                htmlBuilder.append(it.text)
            }
        }
        val doc = Jsoup.parse(htmlBuilder.toString().trimStart())
        doc.body().childNodes().forEach {
            convertHtmlNode(it)
        }
    }

    private fun ContentBlock.convertHtmlNode(node: Node) {
        if (node is TextNode) {
            append(ContentText(node.text()))
        } else if (node is Element) {
            val childBlock = createBlock(node)
            node.childNodes().forEach {
                childBlock.convertHtmlNode(it)
            }
            append(childBlock)
        }
    }

    private fun createBlock(element: Element): ContentBlock = when(element.tagName()) {
        "p" -> ContentParagraph()
        "b", "strong" -> ContentStrong()
        "i", "em" -> ContentEmphasis()
        "s", "del" -> ContentStrikethrough()
        "code" -> ContentCode()
        "pre" -> ContentBlockCode()
        "ul" -> ContentUnorderedList()
        "ol" -> ContentOrderedList()
        "li" -> ContentListItem()
        "a" -> createLink(element)
        else -> ContentBlock()
    }

    private fun createLink(element: Element): ContentBlock {
        val docref = element.attr("docref")
        if (docref != null) {
            return ContentNodeLazyLink(docref, { -> refGraph.lookup(docref)})
        }
        val href = element.attr("href")
        if (href != null) {
            return ContentExternalLink(href)
        } else {
            return ContentBlock()
        }
    }

    private fun MutableContent.convertSeeTag(tag: PsiDocTag) {
        val linkElement = tag.linkElement()
        if (linkElement == null) {
            return
        }
        val seeSection = findSectionByTag(ContentTags.SeeAlso) ?: addSection(ContentTags.SeeAlso, null)
        val linkSignature = resolveLink(linkElement)
        val text = ContentText(linkElement.text)
        if (linkSignature != null) {
            val linkNode = ContentNodeLazyLink(tag.valueElement!!.text, { -> refGraph.lookup(linkSignature)})
            linkNode.append(text)
            seeSection.append(linkNode)
        } else {
            seeSection.append(text)
        }
    }

    private fun convertInlineDocTag(tag: PsiInlineDocTag) = when (tag.name) {
        "link", "linkplain" -> {
            val valueElement = tag.linkElement()
            val linkSignature = resolveLink(valueElement)
            if (linkSignature != null) {
                val labelText = tag.dataElements.firstOrNull { it is PsiDocToken }?.text ?: valueElement!!.text
                val link = "<a docref=\"$linkSignature\">${labelText.htmlEscape()}</a>"
                if (tag.name == "link") "<code>$link</code>" else link
            }
            else if (valueElement != null) {
                valueElement.text
            } else {
                ""
            }
        }
        "code", "literal" -> {
            val text = StringBuilder()
            tag.dataElements.forEach { text.append(it.text) }
            val escaped = text.toString().trimStart().htmlEscape()
            if (tag.name == "code") "<code>$escaped</code>" else escaped
        }
        else -> tag.text
    }

    private fun PsiDocTag.linkElement(): PsiElement? =
            valueElement ?: dataElements.firstOrNull { it !is PsiWhiteSpace }

    private fun resolveLink(valueElement: PsiElement?): String? {
        val target = valueElement?.reference?.resolve()
        if (target != null) {
            return getSignature(target)
        }
        return null
    }

    fun PsiDocTag.getSubjectName(): String? {
        if (name == "param" || name == "throws" || name == "exception") {
            return valueElement?.text
        }
        return null
    }
}


private fun getSignature(element: PsiElement?) = when(element) {
    is PsiClass -> element.qualifiedName
    is PsiField -> element.containingClass!!.qualifiedName + "#" + element.name
    is PsiMethod ->
        element.containingClass!!.qualifiedName + "#" + element.name + "(" +
                element.parameterList.parameters.map { it.type.typeSignature() }.joinToString(",") + ")"
    else -> null
}

private fun PsiType.typeSignature(): String = when(this) {
    is PsiArrayType -> "Array<${componentType.typeSignature()}>"
    else -> mapTypeName(this)
}

private fun mapTypeName(psiType: PsiType): String = when (psiType) {
    PsiType.VOID -> "Unit"
    is PsiPrimitiveType -> psiType.canonicalText.capitalize()
    is PsiClassType -> {
        val psiClass = psiType.resolve()
        if (psiClass?.qualifiedName == "java.lang.Object") "Any" else psiType.className
    }
    is PsiEllipsisType -> mapTypeName(psiType.componentType)
    is PsiArrayType -> "Array"
    else -> psiType.canonicalText
}

class JavaDocumentationBuilder(private val options: DocumentationOptions,
                               private val refGraph: NodeReferenceGraph,
                               private val docParser: JavaDocumentationParser = JavadocParser(refGraph)) {
    fun appendFile(file: PsiJavaFile, module: DocumentationModule) {
        if (file.classes.all { skipElement(it) }) {
            return
        }
        val packageNode = module.findOrCreatePackageNode(file.packageName, emptyMap())
        appendClasses(packageNode, file.classes)
    }

    fun appendClasses(packageNode: DocumentationNode, classes: Array<PsiClass>) {
        packageNode.appendChildren(classes) { build() }
    }

    fun register(element: PsiElement, node: DocumentationNode) {
        val signature = getSignature(element)
        if (signature != null) {
            refGraph.register(signature, node)
        }
    }

    fun link(node: DocumentationNode, element: PsiElement?) {
        val qualifiedName = getSignature(element)
        if (qualifiedName != null) {
            refGraph.link(node, qualifiedName, DocumentationReference.Kind.Link)
        }
    }

    fun link(element: PsiElement?, node: DocumentationNode, kind: DocumentationReference.Kind) {
        val qualifiedName = getSignature(element)
        if (qualifiedName != null) {
            refGraph.link(qualifiedName, node, kind)
        }
    }
    fun DocumentationNode(element: PsiNamedElement,
                          kind: Kind,
                          name: String = element.name ?: "<anonymous>"): DocumentationNode {
        val (docComment, deprecatedContent) = docParser.parseDocumentation(element)
        val node = DocumentationNode(name, docComment, kind)
        if (element is PsiModifierListOwner) {
            node.appendModifiers(element)
            val modifierList = element.modifierList
            if (modifierList != null) {
                modifierList.annotations.filter { !ignoreAnnotation(it) }.forEach {
                    val annotation = it.build()
                    node.append(annotation,
                            if (it.qualifiedName == "java.lang.Deprecated") DocumentationReference.Kind.Deprecation else DocumentationReference.Kind.Annotation)
                }
            }
        }
        if (deprecatedContent != null) {
            val deprecationNode = DocumentationNode("", deprecatedContent, Kind.Modifier)
            node.append(deprecationNode, DocumentationReference.Kind.Deprecation)
        }
        return node
    }

    fun ignoreAnnotation(annotation: PsiAnnotation) = when(annotation.qualifiedName) {
        "java.lang.SuppressWarnings" -> true
        else -> false
    }

    fun <T : Any> DocumentationNode.appendChildren(elements: Array<T>,
                                                   kind: DocumentationReference.Kind = DocumentationReference.Kind.Member,
                                                   buildFn: T.() -> DocumentationNode) {
        elements.forEach {
            if (!skipElement(it)) {
                append(it.buildFn(), kind)
            }
        }
    }

    private fun skipElement(element: Any) = skipElementByVisibility(element) || hasSuppressTag(element)

    private fun skipElementByVisibility(element: Any): Boolean =
        !options.includeNonPublic && element is PsiModifierListOwner &&
                (element.hasModifierProperty(PsiModifier.PRIVATE) || element.hasModifierProperty(PsiModifier.PACKAGE_LOCAL))

    private fun hasSuppressTag(element: Any) =
        element is PsiDocCommentOwner && element.docComment?.let { it.findTagByName("suppress") != null } ?: false

    fun <T : Any> DocumentationNode.appendMembers(elements: Array<T>, buildFn: T.() -> DocumentationNode) =
            appendChildren(elements, DocumentationReference.Kind.Member, buildFn)

    fun <T : Any> DocumentationNode.appendDetails(elements: Array<T>, buildFn: T.() -> DocumentationNode) =
            appendChildren(elements, DocumentationReference.Kind.Detail, buildFn)

    fun PsiClass.build(): DocumentationNode {
        val kind = when {
            isInterface -> DocumentationNode.Kind.Interface
            isEnum -> DocumentationNode.Kind.Enum
            isAnnotationType -> DocumentationNode.Kind.AnnotationClass
            else -> DocumentationNode.Kind.Class
        }
        val node = DocumentationNode(this, kind)
        superTypes.filter { !ignoreSupertype(it) }.forEach {
            node.appendType(it, Kind.Supertype)
            val superClass = it.resolve()
            if (superClass != null) {
                link(superClass, node, DocumentationReference.Kind.Inheritor)
            }
        }
        node.appendDetails(typeParameters) { build() }
        node.appendMembers(methods) { build() }
        node.appendMembers(fields) { build() }
        node.appendMembers(innerClasses) { build() }
        register(this, node)
        return node
    }

    fun ignoreSupertype(psiType: PsiClassType): Boolean =
            psiType.isClass("java.lang.Enum") || psiType.isClass("java.lang.Object")

    fun PsiClassType.isClass(qName: String): Boolean {
        val shortName = qName.substringAfterLast('.')
        if (className == shortName) {
            val psiClass = resolve()
            return psiClass?.qualifiedName == qName
        }
        return false
    }

    fun PsiField.build(): DocumentationNode {
        val node = DocumentationNode(this, nodeKind())
        if (!hasModifierProperty(PsiModifier.FINAL)) {
            node.appendTextNode("var", Kind.Modifier)
        }
        node.appendType(type)
        register(this, node)
        return node
    }

    private fun PsiField.nodeKind(): Kind = when {
        this is PsiEnumConstant -> Kind.EnumItem
        hasModifierProperty(PsiModifier.STATIC) -> Kind.CompanionObjectProperty
        else -> Kind.Property
    }

    fun PsiMethod.build(): DocumentationNode {
        val node = DocumentationNode(this, nodeKind(),
                if (isConstructor) "<init>" else name)

        if (!isConstructor) {
            node.appendType(returnType)
        }
        node.appendDetails(parameterList.parameters) { build() }
        node.appendDetails(typeParameters) { build() }
        register(this, node)
        return node
    }

    private fun PsiMethod.nodeKind(): Kind = when {
        isConstructor -> Kind.Constructor
        hasModifierProperty(PsiModifier.STATIC) -> Kind.CompanionObjectFunction
        else -> Kind.Function
    }

    fun PsiParameter.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Parameter)
        node.appendType(type)
        if (type is PsiEllipsisType) {
            node.appendTextNode("vararg", Kind.Annotation, DocumentationReference.Kind.Annotation)
        }
        return node
    }

    fun PsiTypeParameter.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.TypeParameter)
        extendsListTypes.forEach { node.appendType(it, Kind.UpperBound) }
        implementsListTypes.forEach { node.appendType(it, Kind.UpperBound) }
        return node
    }

    fun DocumentationNode.appendModifiers(element: PsiModifierListOwner) {
        val modifierList = element.modifierList ?: return

        PsiModifier.MODIFIERS.forEach {
            if (it != "static" && modifierList.hasExplicitModifier(it)) {
                appendTextNode(it, Kind.Modifier)
            }
        }
        if ((element is PsiClass || (element is PsiMethod && !element.isConstructor)) &&
                !element.hasModifierProperty(PsiModifier.FINAL)) {
            appendTextNode("open", Kind.Modifier)
        }
    }

    fun DocumentationNode.appendType(psiType: PsiType?, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type) {
        if (psiType == null) {
            return
        }
        append(psiType.build(kind), DocumentationReference.Kind.Detail)
    }

    fun PsiType.build(kind: DocumentationNode.Kind = DocumentationNode.Kind.Type): DocumentationNode {
        val name = mapTypeName(this)
        val node = DocumentationNode(name, Content.Empty, kind)
        if (this is PsiClassType) {
            node.appendDetails(parameters) { build(Kind.Type) }
            link(node, resolve())
        }
        if (this is PsiArrayType && this !is PsiEllipsisType) {
            node.append(componentType.build(Kind.Type), DocumentationReference.Kind.Detail)
        }
        return node
    }

    fun PsiAnnotation.build(): DocumentationNode {
        val node = DocumentationNode(nameReferenceElement?.text ?: "<?>", Content.Empty, DocumentationNode.Kind.Annotation)
        parameterList.attributes.forEach {
            val parameter = DocumentationNode(it.name ?: "value", Content.Empty, DocumentationNode.Kind.Parameter)
            val value = it.value
            if (value != null) {
                val valueText = (value as? PsiLiteralExpression)?.value as? String ?: value.text
                val valueNode = DocumentationNode(valueText, Content.Empty, DocumentationNode.Kind.Value)
                parameter.append(valueNode, DocumentationReference.Kind.Detail)
            }
            node.append(parameter, DocumentationReference.Kind.Detail)
        }
        return node
    }
}
