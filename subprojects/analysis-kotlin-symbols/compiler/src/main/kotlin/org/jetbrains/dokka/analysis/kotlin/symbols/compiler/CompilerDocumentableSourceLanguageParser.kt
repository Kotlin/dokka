package org.jetbrains.dokka.analysis.kotlin.symbols.compiler

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.symbols.KtPsiDocumentableSource
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.kotlin.analysis.kotlin.internal.DocumentableLanguage
import org.jetbrains.kotlin.analysis.kotlin.internal.DocumentableSourceLanguageParser

internal class CompilerDocumentableSourceLanguageParser : DocumentableSourceLanguageParser {
    override fun getLanguage(
        documentable: Documentable,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
    ): DocumentableLanguage? {
        val documentableSource = (documentable as? WithSources)?.sources?.get(sourceSet) ?: return null
        return when (documentableSource) {
            is PsiDocumentableSource -> DocumentableLanguage.JAVA
            is KtPsiDocumentableSource -> DocumentableLanguage.KOTLIN
            else -> error("Unknown language sources: ${documentableSource::class}")
        }
    }
}
