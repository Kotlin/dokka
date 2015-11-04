package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.*
import org.jetbrains.dokka.DocumentationNode.Kind

fun getSignature(element: PsiElement?) = when(element) {
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
    is PsiPrimitiveType -> psiType.canonicalText
    is PsiClassType -> psiType.className
    is PsiEllipsisType -> mapTypeName(psiType.componentType)
    is PsiArrayType -> mapTypeName(psiType.componentType) + "[]"
    else -> psiType.canonicalText
}

interface JavaDocumentationBuilder {
    fun appendFile(file: PsiJavaFile, module: DocumentationModule, packageContent: Map<String, Content>)
}

class JavaPsiDocumentationBuilder : JavaDocumentationBuilder {
    private val options: DocumentationOptions
    private val refGraph: NodeReferenceGraph
    private val docParser: JavaDocumentationParser

    @Inject constructor(options: DocumentationOptions, refGraph: NodeReferenceGraph) {
        this.options = options
        this.refGraph = refGraph
        this.docParser = JavadocParser(refGraph)
    }

    constructor(options: DocumentationOptions, refGraph: NodeReferenceGraph, docParser: JavaDocumentationParser) {
        this.options = options
        this.refGraph = refGraph
        this.docParser = docParser
    }

    override fun appendFile(file: PsiJavaFile, module: DocumentationModule, packageContent: Map<String, Content>) {
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
        node.appendType(type)
        node.appendModifiers(this)
        register(this, node)
        return node
    }

    private fun PsiField.nodeKind(): Kind = when {
        this is PsiEnumConstant -> Kind.EnumItem
        else -> Kind.Field
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
        else -> Kind.Function
    }

    fun PsiParameter.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Parameter)
        node.appendType(type)
        if (type is PsiEllipsisType) {
            node.appendTextNode("vararg", Kind.Modifier, DocumentationReference.Kind.Detail)
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
            if (modifierList.hasExplicitModifier(it)) {
                appendTextNode(it, Kind.Modifier)
            }
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
