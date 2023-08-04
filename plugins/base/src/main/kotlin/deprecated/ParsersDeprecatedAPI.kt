@file:Suppress("PackageDirectoryMismatch", "DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith", "unused")

package org.jetbrains.dokka.base.parsers

import org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE
import org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.TagWrapper

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
abstract class Parser {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    open fun parseStringToDocNode(extractedString: String): DocTag = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    open fun preparse(text: String): String = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    open fun parseTagWithBody(tagName: String, content: String): TagWrapper = throw AnalysisApiDeprecatedError()
}

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
open class MarkdownParser(
    private val externalDri: (String) -> DRI?,
    private val kdocLocation: String?,
) : Parser() {
    companion object {
        @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
        fun parseFromKDocTag(
            @Suppress("UNUSED_PARAMETER") kDocTag: Any?,
            @Suppress("UNUSED_PARAMETER") externalDri: (String) -> DRI?,
            @Suppress("UNUSED_PARAMETER") kdocLocation: String?,
            @Suppress("UNUSED_PARAMETER") parseWithChildren: Boolean = true
        ): DocumentationNode = throw AnalysisApiDeprecatedError()
    }
}
