package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.utilities.DokkaLogger

class JavadocDocumentableToPageTranslator(
    private val context: DokkaContext,
    private val signatureProvider: SignatureProvider,
    private val logger: DokkaLogger
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): RootPageNode =
        JavadocPageCreator(context, signatureProvider, logger).pageForModule(module)
}