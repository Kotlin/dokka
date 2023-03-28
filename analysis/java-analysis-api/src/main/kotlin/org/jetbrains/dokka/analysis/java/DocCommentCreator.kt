package org.jetbrains.dokka.analysis.java

import com.intellij.psi.PsiNamedElement

interface DocCommentCreator {
    fun create(element: PsiNamedElement): DocComment?
}
