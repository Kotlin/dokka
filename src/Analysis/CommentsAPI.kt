package org.jetbrains.dokka

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.kdoc.psi.api.*

/**
 * Retrieves PSI elements representing documentation from the [DeclarationDescriptor]
 *
 * $$receiver: [DeclarationDescriptor] to get documentation nodes from
 */
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

/**
 * Extracts text from KDoc, removes comment symbols and trims whitespace
 */
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
            comment.substring(0, comment.length() - 2)
        else
            comment).trim()
    }.join("\n").trim("\n")
}