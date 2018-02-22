package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import java.io.File

fun getSignature(element: PsiElement?) = when(element) {
    is PsiClass -> element.qualifiedName
    is PsiField -> element.containingClass!!.qualifiedName + "$" + element.name
    is PsiMethod ->
        element.containingClass!!.qualifiedName + "$" + element.name + "(" +
                element.parameterList.parameters.map { it.type.typeSignature() }.joinToString(",") + ")"
    else -> null
}

private fun PsiType.typeSignature(): String = when(this) {
    is PsiArrayType -> "Array((${componentType.typeSignature()}))"
    is PsiPrimitiveType -> "kotlin." + canonicalText.capitalize()
    else -> mapTypeName(this)
}

private fun mapTypeName(psiType: PsiType): String = when (psiType) {
    is PsiPrimitiveType -> psiType.canonicalText
    is PsiClassType -> psiType.resolve()?.qualifiedName ?: psiType.className
    is PsiEllipsisType -> mapTypeName(psiType.componentType)
    is PsiArrayType -> "kotlin.Array"
    else -> psiType.canonicalText
}

interface JavaDocumentationBuilder {
    fun appendFile(file: PsiJavaFile, module: DocumentationModule, packageContent: Map<String, Content>)
}

class JavaPsiDocumentationBuilder : JavaDocumentationBuilder {
    private val options: DocumentationOptions
    private val refGraph: NodeReferenceGraph
    private val docParser: JavaDocumentationParser

    @Inject constructor(options: DocumentationOptions, refGraph: NodeReferenceGraph, logger: DokkaLogger) {
        this.options = options
        this.refGraph = refGraph
        this.docParser = JavadocParser(refGraph, logger)
    }

    constructor(options: DocumentationOptions, refGraph: NodeReferenceGraph, docParser: JavaDocumentationParser) {
        this.options = options
        this.refGraph = refGraph
        this.docParser = docParser
    }

    override fun appendFile(file: PsiJavaFile, module: DocumentationModule, packageContent: Map<String, Content>) {
        if (skipFile(file) || file.classes.all { skipElement(it) }) {
            return
        }
        val packageNode = module.findOrCreatePackageNode(file.packageName, emptyMap(), refGraph)
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
            refGraph.link(node, qualifiedName, RefKind.Link)
        }
    }

    fun link(element: PsiElement?, node: DocumentationNode, kind: RefKind) {
        val qualifiedName = getSignature(element)
        if (qualifiedName != null) {
            refGraph.link(qualifiedName, node, kind)
        }
    }

    fun nodeForElement(element: PsiNamedElement,
                       kind: NodeKind,
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
                            if (it.qualifiedName == "java.lang.Deprecated") RefKind.Deprecation else RefKind.Annotation)
                }
            }
        }
        if (deprecatedContent != null) {
            val deprecationNode = DocumentationNode("", deprecatedContent, NodeKind.Modifier)
            node.append(deprecationNode, RefKind.Deprecation)
        }
        if (element is PsiDocCommentOwner && element.isDeprecated && node.deprecation == null) {
            val deprecationNode = DocumentationNode("", Content.of(ContentText("Deprecated")), NodeKind.Modifier)
            node.append(deprecationNode, RefKind.Deprecation)
        }
        return node
    }

    fun ignoreAnnotation(annotation: PsiAnnotation) = when(annotation.qualifiedName) {
        "java.lang.SuppressWarnings" -> true
        else -> false
    }

    fun <T : Any> DocumentationNode.appendChildren(elements: Array<T>,
                                                   kind: RefKind = RefKind.Member,
                                                   buildFn: T.() -> DocumentationNode) {
        elements.forEach {
            if (!skipElement(it)) {
                append(it.buildFn(), kind)
            }
        }
    }

    private fun skipFile(javaFile: PsiJavaFile): Boolean = options.effectivePackageOptions(javaFile.packageName).suppress

    private fun skipElement(element: Any) =
            skipElementByVisibility(element) ||
                    hasSuppressDocTag(element) ||
                    skipElementBySuppressedFiles(element)

    private fun skipElementByVisibility(element: Any): Boolean = element is PsiModifierListOwner &&
            !(options.effectivePackageOptions((element.containingFile as? PsiJavaFile)?.packageName ?: "").includeNonPublic) &&
            (element.hasModifierProperty(PsiModifier.PRIVATE) ||
                    element.hasModifierProperty(PsiModifier.PACKAGE_LOCAL) ||
                    element.isInternal())

    private fun skipElementBySuppressedFiles(element: Any): Boolean =
            element is PsiElement && File(element.containingFile.virtualFile.path).absoluteFile in options.suppressedFiles

    private fun PsiElement.isInternal(): Boolean {
        val ktElement = (this as? KtLightElement<*, *>)?.kotlinOrigin ?: return false
        return (ktElement as? KtModifierListOwner)?.hasModifier(KtTokens.INTERNAL_KEYWORD) ?: false
    }

    fun <T : Any> DocumentationNode.appendMembers(elements: Array<T>, buildFn: T.() -> DocumentationNode) =
            appendChildren(elements, RefKind.Member, buildFn)

    fun <T : Any> DocumentationNode.appendDetails(elements: Array<T>, buildFn: T.() -> DocumentationNode) =
            appendChildren(elements, RefKind.Detail, buildFn)

    fun PsiClass.build(): DocumentationNode {
        val kind = when {
            isInterface -> NodeKind.Interface
            isEnum -> NodeKind.Enum
            isAnnotationType -> NodeKind.AnnotationClass
            isException() -> NodeKind.Exception
            else -> NodeKind.Class
        }
        val node = nodeForElement(this, kind)
        superTypes.filter { !ignoreSupertype(it) }.forEach {
            node.appendType(it, NodeKind.Supertype)
            val superClass = it.resolve()
            if (superClass != null) {
                link(superClass, node, RefKind.Inheritor)
            }
        }
        node.appendDetails(typeParameters) { build() }
        node.appendMembers(methods) { build() }
        node.appendMembers(fields) { build() }
        node.appendMembers(innerClasses) { build() }
        register(this, node)
        return node
    }

    fun PsiClass.isException() = InheritanceUtil.isInheritor(this, "java.lang.Throwable")

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
        val node = nodeForElement(this, nodeKind())
        node.appendType(type)
        node.appendModifiers(this)
        register(this, node)
        return node
    }

    private fun PsiField.nodeKind(): NodeKind = when {
        this is PsiEnumConstant -> NodeKind.EnumItem
        else -> NodeKind.Field
    }

    fun PsiMethod.build(): DocumentationNode {
        val node = nodeForElement(this, nodeKind(),
                if (isConstructor) "<init>" else name)

        if (!isConstructor) {
            node.appendType(returnType)
        }
        node.appendDetails(parameterList.parameters) { build() }
        node.appendDetails(typeParameters) { build() }
        register(this, node)
        return node
    }

    private fun PsiMethod.nodeKind(): NodeKind = when {
        isConstructor -> NodeKind.Constructor
        else -> NodeKind.Function
    }

    fun PsiParameter.build(): DocumentationNode {
        val node = nodeForElement(this, NodeKind.Parameter)
        node.appendType(type)
        if (type is PsiEllipsisType) {
            node.appendTextNode("vararg", NodeKind.Modifier, RefKind.Detail)
        }
        return node
    }

    fun PsiTypeParameter.build(): DocumentationNode {
        val node = nodeForElement(this, NodeKind.TypeParameter)
        extendsListTypes.forEach { node.appendType(it, NodeKind.UpperBound) }
        implementsListTypes.forEach { node.appendType(it, NodeKind.UpperBound) }
        return node
    }

    fun DocumentationNode.appendModifiers(element: PsiModifierListOwner) {
        val modifierList = element.modifierList ?: return

        PsiModifier.MODIFIERS.forEach {
            if (modifierList.hasExplicitModifier(it)) {
                appendTextNode(it, NodeKind.Modifier)
            }
        }
    }

    fun DocumentationNode.appendType(psiType: PsiType?, kind: NodeKind = NodeKind.Type) {
        if (psiType == null) {
            return
        }
        append(psiType.build(kind), RefKind.Detail)
    }

    fun PsiType.build(kind: NodeKind = NodeKind.Type): DocumentationNode {
        val name = mapTypeName(this)
        val node = DocumentationNode(name, Content.Empty, kind)
        if (this is PsiClassType) {
            node.appendDetails(parameters) { build(NodeKind.Type) }
            link(node, resolve())
        }
        if (this is PsiArrayType && this !is PsiEllipsisType) {
            node.append(componentType.build(NodeKind.Type), RefKind.Detail)
        }
        return node
    }

    fun PsiAnnotation.build(): DocumentationNode {
        val node = DocumentationNode(nameReferenceElement?.text ?: "<?>", Content.Empty, NodeKind.Annotation)
        parameterList.attributes.forEach {
            val parameter = DocumentationNode(it.name ?: "value", Content.Empty, NodeKind.Parameter)
            val value = it.value
            if (value != null) {
                val valueText = (value as? PsiLiteralExpression)?.value as? String ?: value.text
                val valueNode = DocumentationNode(valueText, Content.Empty, NodeKind.Value)
                parameter.append(valueNode, RefKind.Detail)
            }
            node.append(parameter, RefKind.Detail)
        }
        return node
    }
}

fun hasSuppressDocTag(element: Any?): Boolean {
    val declaration = (element as? KtLightDeclaration<*, *>)?.kotlinOrigin as? KtDeclaration ?: return false
    return PsiTreeUtil.findChildrenOfType(declaration.docComment, KDocTag::class.java).any { it.knownTag == KDocKnownTag.SUPPRESS }
}

