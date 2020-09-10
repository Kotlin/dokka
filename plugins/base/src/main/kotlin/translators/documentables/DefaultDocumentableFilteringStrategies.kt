package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.PrimaryConstructorExtra

object DefaultDocumentableFilteringStrategies : DocumentableFilteringStrategies {
    override fun shouldConstructorBeInPage(constructor: DFunction): Boolean =
        constructor.extra[PrimaryConstructorExtra] == null || constructor.documentation.isNotEmpty()
}
