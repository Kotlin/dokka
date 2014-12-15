package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.kdoc.psi.api.*
import org.jetbrains.jet.lang.psi.*

fun DeclarationDescriptor.getDocumentationElements(): List<KDoc> {
    val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(this)
    if (psiElement == null)
        return listOf()

    return psiElement.children() // visit children
            .takeWhile { it is KDoc } // all KDoc
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
        val comment = it.trim().dropWhile { it == '/' || it == '*' }
        (if (comment.endsWith("*/"))
            comment.substring(0, comment.length - 2)
        else
            comment).trim()
    }.join("\n")
}