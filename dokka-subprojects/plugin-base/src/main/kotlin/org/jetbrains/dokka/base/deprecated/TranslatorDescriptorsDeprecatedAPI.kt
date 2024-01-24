/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch", "DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith", "unused")

package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE
import org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public fun interface ExternalDocumentablesProvider {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public fun findClasslike(dri: DRI, sourceSet: DokkaConfiguration.DokkaSourceSet): DClasslike?
}

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public class DefaultExternalDocumentablesProvider(
    @Suppress("UNUSED_PARAMETER") context: DokkaContext
) : ExternalDocumentablesProvider {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    override fun findClasslike(dri: DRI, sourceSet: DokkaConfiguration.DokkaSourceSet): DClasslike =
        throw AnalysisApiDeprecatedError()
}

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public fun interface ExternalClasslikesTranslator {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public fun translateClassDescriptor(descriptor: Any, sourceSet: DokkaConfiguration.DokkaSourceSet): DClasslike
}

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public class DefaultDescriptorToDocumentableTranslator(
    private val context: DokkaContext
) : AsyncSourceToDocumentableTranslator, ExternalClasslikesTranslator {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    override suspend fun invokeSuspending(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext, ): DModule =
        throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    override fun translateClassDescriptor(descriptor: Any, sourceSet: DokkaConfiguration.DokkaSourceSet): DClasslike =
        throw AnalysisApiDeprecatedError()
}
