@file:Suppress("PackageDirectoryMismatch", "DeprecatedCallableAddReplaceWith", "unused")

package org.jetbrains.dokka.base.translators.psi

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE
import org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
class DefaultPsiToDocumentableTranslator(
    @Suppress("UNUSED_PARAMETER") context: DokkaContext,
) : AsyncSourceToDocumentableTranslator {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    override suspend fun invokeSuspending(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        context: DokkaContext,
    ): DModule = throw AnalysisApiDeprecatedError()
}
