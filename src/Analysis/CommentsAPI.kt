package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.kdoc.psi.api.*
import org.jetbrains.jet.lang.psi.*

fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): String {
    return getDocumentationElements(descriptor).map { it.extractText() }.join("\n")
}

fun BindingContext.getDocumentationElements(descriptor: DeclarationDescriptor): List<KDoc> {
    val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
    if (psiElement == null)
        throw IllegalArgumentException("$descriptor doesn't have connection to source code, is it synthetic?")

    return psiElement.previousSiblings() // go backwards
            .takeWhile { it !is JetDeclaration } // till previous declaration
            .filter { it is KDoc } // get KDocs
            .map { it as KDoc } // cast
            .toList()
            .reverse() // make reversed list
}

fun KDoc?.extractText(): String {
    if (this == null)
        return ""
    val text = getText()
    if (text == null)
        return ""
    val lines = text.replace("\r", "").split("\n")
    return lines.map {
        val comment = it.trim()
                .dropWhile { it == '/' }
                .dropWhile { it == '*' }
                .dropWhile { it == '/' }
                .trim()
        if (comment.endsWith("*/"))
            comment.substring(0, comment.length - 2).trim()
        else
            comment
    }.filter { it.any() }.join("\n")
}