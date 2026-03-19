/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.DRI

/**
 * Shared utilities for extracting documentable information from PSI elements.
 * Used by both `DokkaPsiParser` (PSI analysis) and `DokkaSymbolVisitor` (AA analysis).
 */
@InternalDokkaApi
public object PsiDocumentableUtils {

    /**
     * Extracts a list of [DRI]s for checked exceptions from a [PsiMethod]'s throws clause.
     */
    public fun getCheckedExceptionDRIs(psiMethod: PsiMethod): List<DRI> =
        psiMethod.throwsList.toDriList()

    private fun PsiReferenceList.toDriList(): List<DRI> =
        referenceElements.mapNotNull { it?.resolve()?.let { resolved -> DRI.from(resolved) } }
}
