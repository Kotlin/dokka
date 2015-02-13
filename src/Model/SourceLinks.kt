package org.jetbrains.dokka

import com.intellij.psi.PsiElement
import java.io.File
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiNameIdentifierOwner

class SourceLinkDefinition(val path: String, val url: String, val lineSuffix: String?)

fun DocumentationNode.appendSourceLink(psi: PsiElement?, sourceLinks: List<SourceLinkDefinition>) {
    val path = psi?.getContainingFile()?.getVirtualFile()?.getPath()
    if (path == null) {
        return
    }
    val target = if (psi is PsiNameIdentifierOwner) psi.getNameIdentifier() else psi
    val absPath = File(path).getAbsolutePath()
    val linkDef = sourceLinks.firstOrNull { absPath.startsWith(it.path) }
    if (linkDef != null) {
        var url = linkDef.url + path.substring(linkDef.path.length())
        if (linkDef.lineSuffix != null) {
            val doc = PsiDocumentManager.getInstance(psi!!.getProject()).getDocument(psi.getContainingFile())
            if (doc != null) {
                // IJ uses 0-based line-numbers; external source browsers use 1-based
                val line = doc.getLineNumber(target!!.getTextRange().getStartOffset()) + 1
                url += linkDef.lineSuffix + line.toString()
            }
        }
        append(DocumentationNode(url, Content.Empty, DocumentationNode.Kind.SourceUrl),
                DocumentationReference.Kind.Detail);
    }
}
