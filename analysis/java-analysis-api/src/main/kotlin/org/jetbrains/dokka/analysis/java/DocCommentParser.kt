package org.jetbrains.dokka.analysis.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.model.doc.DocumentationNode

interface DocCommentParser {
    fun canParse(docComment: DocComment): Boolean
    fun parse(docComment: DocComment, context: PsiNamedElement): DocumentationNode
}
