package org.jetbrains.dokka

import com.intellij.psi.PsiElement
import java.io.File
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class SourceLinkDefinition(val path: String, val url: String, val lineSuffix: String?)

fun DocumentationNode.appendSourceLink(psi: PsiElement?, sourceLinks: List<SourceLinkDefinition>) {
    val path = psi?.containingFile?.virtualFile?.path ?: return

    val target = if (psi is PsiNameIdentifierOwner) psi.nameIdentifier else psi
    val absPath = File(path).absolutePath
    val linkDef = sourceLinks.firstOrNull { absPath.startsWith(it.path) }
    if (linkDef != null) {
        var url = linkDef.url + path.substring(linkDef.path.length())
        if (linkDef.lineSuffix != null) {
            val line = target?.lineNumber()
            if (line != null) {
                url += linkDef.lineSuffix + line.toString()
            }
        }
        append(DocumentationNode(url, Content.Empty, DocumentationNode.Kind.SourceUrl),
                DocumentationReference.Kind.Detail);
    }

    append(DocumentationNode("$path:${target?.lineNumber()}:${target?.columnNumber()}", Content.Empty, DocumentationNode.Kind.SourcePosition), DocumentationReference.Kind.Detail)
}

fun PsiElement.lineNumber(): Int? {
    val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile)
    // IJ uses 0-based line-numbers; external source browsers use 1-based
    return doc?.getLineNumber(textRange.startOffset)?.plus(1)
}

fun PsiElement.columnNumber(): Int? {
    val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile)
    // IJ uses 0-based line-numbers; external source browsers use 1-based
    val lineNumber = doc?.getLineNumber(textRange.startOffset)?.plus(1) ?: return null
    return startOffset - doc!!.getLineStartOffset(lineNumber)
}