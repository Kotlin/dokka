package org.jetbrains.dokka.transformers.psi

import com.intellij.psi.PsiJavaFile
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext

interface PsiToDocumentationTranslator {
    fun invoke(
        moduleName: String,
        psiFiles: List<PsiJavaFile>,
        platformData: PlatformData,
        context: DokkaContext
    ): Module
}
