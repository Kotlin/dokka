package org.jetbrains.dokka.transformers.psi

import com.intellij.psi.PsiJavaFile
import org.jetbrains.dokka.model.DModuleView
import org.jetbrains.dokka.model.DPass
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext

interface PsiToDocumentableTranslator {
    fun invoke(
        moduleName: String,
        psiFiles: List<PsiJavaFile>,
        platformData: PlatformData,
        context: DokkaContext
    ): DPass
}
