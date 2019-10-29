package org.jetbrains.dokka

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.dokka.DokkaConfiguration.SourceLinkDefinition
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File

fun PsiElement.lineNumber(): Int? {
    val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile)
    // IJ uses 0-based line-numbers; external source browsers use 1-based
    return doc?.getLineNumber(textRange.startOffset)?.plus(1)
}