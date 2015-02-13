package org.jetbrains.dokka

import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiClass
import org.jetbrains.dokka.DocumentationNode.Kind
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.PsiType
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiModifier

public class JavaDocumentationBuilder() {
    fun appendFile(file: PsiJavaFile, module: DocumentationModule) {
        val packageNode = module.findOrCreatePackageNode(file.getPackageName())
        packageNode.appendChildren(file.getClasses()) { build() }
    }

    fun parseDocumentation(docComment: PsiDocComment?): Content {
        if (docComment == null) return Content.Empty
        val result = Content()
        docComment.getDescriptionElements().dropWhile { it.getText().trim().isEmpty() }.forEach {
            val text = if (result.isEmpty()) it.getText().trimLeading() else it.getText()
            result.append(ContentText(text))
        }
        return result
    }

    fun DocumentationNode(element: PsiNamedElement, kind: Kind): DocumentationNode {
        val docComment = if (element is PsiDocCommentOwner) parseDocumentation(element.getDocComment()) else Content.Empty
        val node = DocumentationNode(element.getName() ?: "<anonymous>", docComment, kind)
        if (element is PsiModifierListOwner) {
            node.appendModifiers(element)
        }
        return node
    }

    fun DocumentationNode.appendChildren<T>(elements: Array<T>,
                                            kind: DocumentationReference.Kind = DocumentationReference.Kind.Member,
                                            buildFn: T.() -> DocumentationNode) {
        elements.forEach {
            append(it.buildFn(), kind)
        }
    }

    fun PsiClass.build(): DocumentationNode {
        val kind = when {
            isInterface() -> DocumentationNode.Kind.Interface
            isEnum() -> DocumentationNode.Kind.Enum
            isAnnotationType() -> DocumentationNode.Kind.AnnotationClass
            else -> DocumentationNode.Kind.Class
        }
        val node = DocumentationNode(this, kind)
        getExtendsListTypes().forEach { node.appendType(it, Kind.Supertype) }
        getImplementsListTypes().forEach { node.appendType(it, Kind.Supertype) }
        node.appendChildren(getMethods()) { build() }
        return node
    }

    fun PsiMethod.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Function)
        node.appendType(getReturnType())
        node.appendChildren(getParameterList().getParameters(), DocumentationReference.Kind.Detail) { build() }
        return node
    }

    fun PsiParameter.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Parameter)
        node.appendType(getType())
        return node
    }

    fun DocumentationNode.appendModifiers(element: PsiModifierListOwner) {
        val modifierList = element.getModifierList()
        if (modifierList == null) {
            return
        }
        PsiModifier.MODIFIERS.forEach {
            if (modifierList.hasExplicitModifier(it)) {
                val modifierNode = DocumentationNode(it, Content.Empty, Kind.Modifier)
                append(modifierNode, DocumentationReference.Kind.Detail)
            }
        }
    }

    fun DocumentationNode.appendType(psiType: PsiType?, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type) {
        if (psiType == null) {
            return
        }
        val name = mapTypeName(psiType)
        val node = DocumentationNode(name, Content.Empty, kind)
        append(node, DocumentationReference.Kind.Detail)
    }

    private fun mapTypeName(psiType: PsiType): String = when (psiType) {
        PsiType.VOID -> "Unit"
        is PsiPrimitiveType -> psiType.getCanonicalText().capitalize()
        is PsiClassType -> psiType.getClassName()
        else -> psiType.getCanonicalText()
    }
}
