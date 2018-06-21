package org.jetbrains.dokka

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.dokka.DokkaConfiguration.SourceLinkDefinition
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File


fun DocumentationNode.appendSourceLink(psi: PsiElement?, sourceLinks: List<SourceLinkDefinition>) {
    val path = psi?.containingFile?.virtualFile?.path ?: return
    val canonicalPath = File(path).canonicalPath

    val target = if (psi is PsiNameIdentifierOwner) psi.nameIdentifier else psi
    val pair = determineSourceLinkDefinition(canonicalPath, sourceLinks)
    if (pair != null) {
        val (sourceLinkDefinition, sourceLinkCanonicalPath) = pair
        var url = determineUrl(canonicalPath, sourceLinkDefinition, sourceLinkCanonicalPath)
        if (sourceLinkDefinition.lineSuffix != null) {
            val line = target?.lineNumber()
            if (line != null) {
                url += sourceLinkDefinition.lineSuffix + line.toString()
            }
        }
        append(DocumentationNode(url, Content.Empty, NodeKind.SourceUrl), RefKind.Detail)
    }

    if (target != null) {
        append(DocumentationNode(target.sourcePosition(), Content.Empty, NodeKind.SourcePosition), RefKind.Detail)
    }
}

private fun determineSourceLinkDefinition(
    canonicalPath: String,
    sourceLinks: List<SourceLinkDefinition>
): Pair<SourceLinkDefinition, String>? {
    return sourceLinks
        .asSequence()
        .map { it to File(it.path).canonicalPath }
        .firstOrNull { (_, sourceLinkCanonicalPath) ->
            canonicalPath.startsWith(sourceLinkCanonicalPath)
        }
}

private fun determineUrl(
    canonicalPath: String,
    sourceLinkDefinition: SourceLinkDefinition,
    sourceLinkCanonicalPath: String
): String {
    val relativePath = canonicalPath.substring(sourceLinkCanonicalPath.length)
    val relativeUrl = relativePath.replace('\\', '/').removePrefix("/")
    return "${sourceLinkDefinition.url.removeSuffix("/")}/$relativeUrl"
}

private fun PsiElement.sourcePosition(): String {
    val path = containingFile.virtualFile.path
    val lineNumber = lineNumber()
    val columnNumber = columnNumber()

    return when {
        lineNumber == null -> path
        columnNumber == null -> "$path:$lineNumber"
        else -> "$path:$lineNumber:$columnNumber"
    }
}

fun PsiElement.lineNumber(): Int? {
    val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile)
    // IJ uses 0-based line-numbers; external source browsers use 1-based
    return doc?.getLineNumber(textRange.startOffset)?.plus(1)
}

fun PsiElement.columnNumber(): Int? {
    val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile) ?: return null
    val lineNumber = doc.getLineNumber(textRange.startOffset)
    return startOffset - doc.getLineStartOffset(lineNumber)
}