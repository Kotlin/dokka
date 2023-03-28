package org.jetbrains.dokka.analysis.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.doc.DocumentationNode

@InternalDokkaApi
interface DocCommentParser {
    fun canParse(docComment: DocComment): Boolean
    fun parse(docComment: DocComment, context: PsiNamedElement): DocumentationNode
}
