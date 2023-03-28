package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.PsiElement

// TODO [beresnev] refactor
internal fun PsiElement.resolveToGetDri(): PsiElement? =
    reference?.resolve()
