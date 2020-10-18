package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator

class JavadocDocumentableToPageTranslator(
    private val context: DokkaContext
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): RootPageNode = JavadocPageCreator(context).pageForModule(module)
}