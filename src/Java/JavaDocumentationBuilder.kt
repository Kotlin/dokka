package org.jetbrains.dokka

import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocTagValue
import com.intellij.psi.javadoc.PsiInlineDocTag
import org.jetbrains.dokka.DocumentationNode.Kind

public class JavaDocumentationBuilder(private val options: DocumentationOptions,
                                      private val refGraph: NodeReferenceGraph) {
    fun appendFile(file: PsiJavaFile, module: DocumentationModule) {
        val packageNode = module.findOrCreatePackageNode(file.getPackageName())
        packageNode.appendChildren(file.getClasses()) { build() }
    }

    data class JavadocParseResult(val content: Content, val deprecatedContent: Content?)

    fun parseDocumentation(docComment: PsiDocComment?): JavadocParseResult {
        if (docComment == null) return JavadocParseResult(Content.Empty, null)
        val result = MutableContent()
        var deprecatedContent: Content? = null
        val para = ContentParagraph()
        result.append(para)
        para.convertJavadocElements(docComment.getDescriptionElements().dropWhile { it.getText().trim().isEmpty() })
        docComment.getTags().forEach { tag ->
            when(tag.getName()) {
                "see" -> result.convertSeeTag(tag)
                "deprecated" -> {
                    deprecatedContent = Content()
                    deprecatedContent!!.convertJavadocElements(tag.getDataElements().toArrayList())
                }
                else -> {
                    val subjectName = tag.getSubjectName()
                    val section = result.addSection(javadocSectionDisplayName(tag.getName()), subjectName)

                    tag.getDataElements().forEach {
                        if (it !is PsiDocTagValue || tag.getSubjectName() == null) {
                            section.append(ContentText(it.getText()))
                        }
                    }
                }
            }
        }
        return JavadocParseResult(result, deprecatedContent)
    }

    private fun ContentBlock.convertJavadocElements(elements: Iterable<PsiElement>) {
        elements.forEach {
            if (it is PsiInlineDocTag) {
                append(convertInlineDocTag(it))
            } else {
                val text = if (isEmpty()) it.getText().trimLeading() else it.getText()
                append(ContentText(text))
            }
        }
    }

    private fun MutableContent.convertSeeTag(tag: PsiDocTag) {
        val seeSection = findSectionByTag("See Also") ?: addSection("See Also", null)
        val linkNode = resolveLink(tag.getValueElement())
        val text = ContentText(tag.getValueElement()!!.getText())
        if (linkNode != null) {
            linkNode.append(text)
            seeSection.append(linkNode)
        } else {
            seeSection.append(text)
        }
    }

    private fun convertInlineDocTag(tag: PsiInlineDocTag) = when (tag.getName()) {
        "link", "linkplain" -> {
            val link = resolveLink(tag.getValueElement())
            if (link != null) {
                link.append(ContentText(tag.getValueElement()!!.getText()))
                link
            }
            else ContentText(tag.getValueElement()!!.getText())
        }
        else -> ContentText(tag.getText())
    }

    private fun resolveLink(valueElement: PsiDocTagValue?): ContentBlock? {
        val target = valueElement?.getReference()?.resolve()
        if (target != null) {
            val signature = getSignature(target)
            if (signature != null) {
                return ContentNodeLazyLink(valueElement!!.getText(), {() -> refGraph.lookup(signature)})
            }
        }
        return null
    }

    fun PsiDocTag.getSubjectName(): String? {
        if (getName() == "param" || getName() == "throws" || getName() == "exception") {
            return getValueElement()?.getText()
        }
        return null
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

    private fun getSignature(element: PsiElement?) = when(element) {
        is PsiClass -> element.getQualifiedName()
        is PsiField -> element.getContainingClass().getQualifiedName() + "#" + element.getName()
        is PsiMethod ->
            element.getContainingClass().getQualifiedName() + "#" + element.getName() + "(" +
            element.getParameterList().getParameters().map { it.getType().typeSignature() }.join(",") + ")"
        else -> null
    }

    private fun PsiType.typeSignature(): String = when(this) {
        is PsiArrayType -> "Array<${getComponentType().typeSignature()}>"
        else -> mapTypeName(this)
    }

    fun DocumentationNode(element: PsiNamedElement,
                          kind: Kind,
                          name: String = element.getName() ?: "<anonymous>"): DocumentationNode {
        val (docComment, deprecatedContent) = parseDocumentation((element as? PsiDocCommentOwner)?.getDocComment())
        val node = DocumentationNode(name, docComment, kind)
        if (element is PsiModifierListOwner) {
            node.appendModifiers(element)
            val modifierList = element.getModifierList()
            if (modifierList != null) {
                modifierList.getAnnotations().filter { !ignoreAnnotation(it) }.forEach {
                    val annotation = it.build()
                    node.append(annotation,
                            if (it.getQualifiedName() == "java.lang.Deprecated") DocumentationReference.Kind.Deprecation else DocumentationReference.Kind.Annotation)
                }
            }
        }
        if (deprecatedContent != null) {
            val deprecationNode = DocumentationNode("", deprecatedContent, Kind.Modifier)
            node.append(deprecationNode, DocumentationReference.Kind.Deprecation)
        }
        return node
    }

    fun ignoreAnnotation(annotation: PsiAnnotation) = when(annotation.getQualifiedName()) {
        "java.lang.SuppressWarnings" -> true
        else -> false
    }

    fun DocumentationNode.appendChildren<T>(elements: Array<T>,
                                            kind: DocumentationReference.Kind = DocumentationReference.Kind.Member,
                                            buildFn: T.() -> DocumentationNode) {
        elements.forEach {
            if (!skipElement(it)) {
                append(it.buildFn(), kind)
            }
        }
    }

    private fun skipElement(element: Any): Boolean =
        !options.includeNonPublic && element is PsiModifierListOwner && element.hasModifierProperty(PsiModifier.PRIVATE)

    fun DocumentationNode.appendMembers<T>(elements: Array<T>, buildFn: T.() -> DocumentationNode) =
            appendChildren(elements, DocumentationReference.Kind.Member, buildFn)

    fun DocumentationNode.appendDetails<T>(elements: Array<T>, buildFn: T.() -> DocumentationNode) =
            appendChildren(elements, DocumentationReference.Kind.Detail, buildFn)

    fun PsiClass.build(): DocumentationNode {
        val kind = when {
            isInterface() -> DocumentationNode.Kind.Interface
            isEnum() -> DocumentationNode.Kind.Enum
            isAnnotationType() -> DocumentationNode.Kind.AnnotationClass
            else -> DocumentationNode.Kind.Class
        }
        val node = DocumentationNode(this, kind)
        getSuperTypes().filter { !ignoreSupertype(it) }.forEach {
            node.appendType(it, Kind.Supertype)
            val superClass = it.resolve()
            if (superClass != null) {
                link(superClass, node, DocumentationReference.Kind.Inheritor)
            }
        }
        node.appendDetails(getTypeParameters()) { build() }
        node.appendMembers(getMethods()) { build() }
        node.appendMembers(getFields()) { build() }
        node.appendMembers(getInnerClasses()) { build() }
        register(this, node)
        return node
    }

    fun ignoreSupertype(psiType: PsiClassType): Boolean =
            psiType.isClass("java.lang.Enum") || psiType.isClass("java.lang.Object")

    fun PsiClassType.isClass(qName: String): Boolean {
        val shortName = qName.substringAfterLast('.')
        if (getClassName() == shortName) {
            val psiClass = resolve()
            return psiClass?.getQualifiedName() == qName
        }
        return false
    }

    fun PsiField.build(): DocumentationNode {
        val node = DocumentationNode(this, nodeKind())
        if (!hasModifierProperty(PsiModifier.FINAL)) {
            node.appendTextNode("var", Kind.Modifier)
        }
        node.appendType(getType())
        register(this, node)
        return node
    }

    private fun PsiField.nodeKind(): Kind = when {
        this is PsiEnumConstant -> Kind.EnumItem
        hasModifierProperty(PsiModifier.STATIC) -> Kind.DefaultObjectProperty
        else -> Kind.Property
    }

    fun PsiMethod.build(): DocumentationNode {
        val node = DocumentationNode(this, nodeKind(),
                if (isConstructor()) "<init>" else getName())

        if (!isConstructor()) {
            node.appendType(getReturnType())
        }
        node.appendDetails(getParameterList().getParameters()) { build() }
        node.appendDetails(getTypeParameters()) { build() }
        register(this, node)
        return node
    }

    private fun PsiMethod.nodeKind(): Kind = when {
        isConstructor() -> Kind.Constructor
        hasModifierProperty(PsiModifier.STATIC) -> Kind.DefaultObjectFunction
        else -> Kind.Function
    }

    fun PsiParameter.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Parameter)
        node.appendType(getType())
        if (getType() is PsiEllipsisType) {
            node.appendTextNode("vararg", Kind.Annotation, DocumentationReference.Kind.Annotation)
        }
        return node
    }

    fun PsiTypeParameter.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.TypeParameter)
        getExtendsListTypes().forEach { node.appendType(it, Kind.UpperBound) }
        getImplementsListTypes().forEach { node.appendType(it, Kind.UpperBound) }
        return node
    }

    fun DocumentationNode.appendModifiers(element: PsiModifierListOwner) {
        val modifierList = element.getModifierList()
        if (modifierList == null) {
            return
        }
        PsiModifier.MODIFIERS.forEach {
            if (it != "static" && modifierList.hasExplicitModifier(it)) {
                appendTextNode(it, Kind.Modifier)
            }
        }
        if ((element is PsiClass || element is PsiMethod) && !element.hasModifierProperty(PsiModifier.FINAL)) {
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
            node.appendDetails(getParameters()) { build(Kind.Type) }
            link(node, resolve())
        }
        if (this is PsiArrayType && this !is PsiEllipsisType) {
            node.append(getComponentType().build(Kind.Type), DocumentationReference.Kind.Detail)
        }
        return node
    }

    private fun mapTypeName(psiType: PsiType): String = when (psiType) {
        PsiType.VOID -> "Unit"
        is PsiPrimitiveType -> psiType.getCanonicalText().capitalize()
        is PsiClassType -> {
            val psiClass = psiType.resolve()
            if (psiClass?.getQualifiedName() == "java.lang.Object") "Any" else psiType.getClassName()
        }
        is PsiEllipsisType -> mapTypeName(psiType.getComponentType())
        is PsiArrayType -> "Array"
        else -> psiType.getCanonicalText()
    }

    fun PsiAnnotation.build(): DocumentationNode {
        val node = DocumentationNode(getNameReferenceElement()?.getText() ?: "<?>", Content.Empty, DocumentationNode.Kind.Annotation)
        getParameterList().getAttributes().forEach {
            val parameter = DocumentationNode(it.getName() ?: "value", Content.Empty, DocumentationNode.Kind.Parameter)
            val value = it.getValue()
            if (value != null) {
                val valueText = (value as? PsiLiteralExpression)?.getValue() as? String ?: value.getText()
                val valueNode = DocumentationNode(valueText, Content.Empty, DocumentationNode.Kind.Value)
                parameter.append(valueNode, DocumentationReference.Kind.Detail)
            }
            node.append(parameter, DocumentationReference.Kind.Detail)
        }
        return node
    }
}
