@file:Suppress("DeprecatedCallableAddReplaceWith", "PackageDirectoryMismatch")

package org.jetbrains.dokka.base.parsers.factories

import org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE
import org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocTag

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
object DocTagsFromStringFactory {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    fun getInstance(
        name: String,
        children: List<DocTag> = emptyList(),
        params: Map<String, String> = emptyMap(),
        body: String? = null,
        dri: DRI? = null,
    ): DocTag = throw AnalysisApiDeprecatedError()
}
