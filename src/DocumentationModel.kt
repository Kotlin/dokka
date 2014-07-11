package com.jetbrains.dokka

import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.resolve.BindingContext

public class DocumentationModel {
    fun merge(other: DocumentationModel): DocumentationModel {
        return DocumentationModel()
    }
}

fun BindingContext.createDocumentation(file: PsiFile): DocumentationModel {
    return DocumentationModel()
}