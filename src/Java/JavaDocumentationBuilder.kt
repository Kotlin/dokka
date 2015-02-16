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
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocTagValue

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
        docComment.getTags().forEach {
            val subjectName = it.getSubjectName()
            val section = result.addSection(javadocSectionDisplayName(it.getName()), subjectName)
            it.getDataElements().forEach {
                if (it !is PsiDocTagValue) {
                    section.append(ContentText(it.getText()))
                }
            }
        }
        return result
    }

    fun PsiDocTag.getSubjectName(): String? {
        if (getName() == "param") {
            return getValueElement()?.getText()
        }
        return null
    }

    fun DocumentationNode(element: PsiNamedElement,
                          kind: Kind,
                          name: String = element.getName() ?: "<anonymous>"): DocumentationNode {
        val docComment = if (element is PsiDocCommentOwner) parseDocumentation(element.getDocComment()) else Content.Empty
        val node = DocumentationNode(name, docComment, kind)
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
        getExtendsListTypes().forEach { node.appendType(it, Kind.Supertype) }
        getImplementsListTypes().forEach { node.appendType(it, Kind.Supertype) }
        node.appendDetails(getTypeParameters()) { build() }
        node.appendMembers(getMethods()) { build() }
        node.appendMembers(getInnerClasses()) { build() }
        return node
    }

    fun PsiMethod.build(): DocumentationNode {
        val node = DocumentationNode(this,
                if (isConstructor()) Kind.Constructor else Kind.Function,
                if (isConstructor()) "<init>" else getName())

        if (!isConstructor()) {
            node.appendType(getReturnType())
        }
        node.appendDetails(getParameterList().getParameters()) { build() }
        node.appendDetails(getTypeParameters()) { build() }
        return node
    }

    fun PsiParameter.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Parameter)
        node.appendType(getType())
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
        append(psiType.build(kind), DocumentationReference.Kind.Detail)
    }

    fun PsiType.build(kind: DocumentationNode.Kind = DocumentationNode.Kind.Type): DocumentationNode {
        val name = mapTypeName(this)
        val node = DocumentationNode(name, Content.Empty, kind)
        if (this is PsiClassType) {
            node.appendDetails(getParameters()) { build(Kind.TypeParameter) }
        }
        return node
    }

    private fun mapTypeName(psiType: PsiType): String = when (psiType) {
        PsiType.VOID -> "Unit"
        is PsiPrimitiveType -> psiType.getCanonicalText().capitalize()
        is PsiClassType -> psiType.getClassName()
        is PsiArrayType -> "Array<${mapTypeName(psiType.getComponentType())}>"
        else -> psiType.getCanonicalText()
    }
}
