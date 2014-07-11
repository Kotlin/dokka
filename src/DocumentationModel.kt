package com.jetbrains.dokka

import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.resolve.BindingContext

public class DocumentationNode {
    val name = "fn"
    val doc = "doc"
}

public class DocumentationModel {
    fun merge(other: DocumentationModel): DocumentationModel {
        return DocumentationModel()
    }

    val items : List<DocumentationNode> = listOf(DocumentationNode())
}

fun BindingContext.createDocumentation(file: PsiFile): DocumentationModel {
    return DocumentationModel()
}