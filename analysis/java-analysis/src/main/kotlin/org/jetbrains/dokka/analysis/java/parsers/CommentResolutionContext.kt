package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.dokka.analysis.java.JavadocTag

data class CommentResolutionContext(
    val comment: PsiDocComment,
    val tag: JavadocTag?
)
