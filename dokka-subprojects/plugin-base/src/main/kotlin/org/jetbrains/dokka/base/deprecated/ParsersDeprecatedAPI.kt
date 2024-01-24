/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch", "DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith", "unused")

package org.jetbrains.dokka.base.parsers

import org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE
import org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.TagWrapper

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public abstract class Parser {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public open fun parseStringToDocNode(extractedString: String): DocTag = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public open fun preparse(text: String): String = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public open fun parseTagWithBody(tagName: String, content: String): TagWrapper = throw AnalysisApiDeprecatedError()
}

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public open class MarkdownParser(
    private val externalDri: (String) -> DRI?,
    private val kdocLocation: String?,
) : Parser() {
    public companion object {
        @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
        public fun parseFromKDocTag(
            @Suppress("UNUSED_PARAMETER") kDocTag: Any?,
            @Suppress("UNUSED_PARAMETER") externalDri: (String) -> DRI?,
            @Suppress("UNUSED_PARAMETER") kdocLocation: String?,
            @Suppress("UNUSED_PARAMETER") parseWithChildren: Boolean = true
        ): DocumentationNode = throw AnalysisApiDeprecatedError()
    }
}
