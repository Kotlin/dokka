package org.jetbrains.dokka.processing.translators.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.processing.signatures.SignatureProvider
import org.jetbrains.dokka.processing.translators.docTags.CommentsToContentTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.utilities.DokkaLogger

class DefaultDocumentableToPageTranslator(
    private val commentsToContentConverter: CommentsToContentTranslator,
    private val signatureProvider: SignatureProvider,
    private val logger: DokkaLogger
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): ModulePageNode =
        DefaultPageCreator(commentsToContentConverter, signatureProvider, logger).pageForModule(module)
}