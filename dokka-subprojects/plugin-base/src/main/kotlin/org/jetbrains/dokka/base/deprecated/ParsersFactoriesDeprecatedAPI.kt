/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith", "PackageDirectoryMismatch", "unused")

package org.jetbrains.dokka.base.parsers.factories

import org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE
import org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocTag

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public object DocTagsFromStringFactory {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public fun getInstance(
        @Suppress("UNUSED_PARAMETER") name: String,
        @Suppress("UNUSED_PARAMETER") children: List<DocTag> = emptyList(),
        @Suppress("UNUSED_PARAMETER") params: Map<String, String> = emptyMap(),
        @Suppress("UNUSED_PARAMETER") body: String? = null,
        @Suppress("UNUSED_PARAMETER") dri: DRI? = null,
    ): DocTag = throw AnalysisApiDeprecatedError()
}
