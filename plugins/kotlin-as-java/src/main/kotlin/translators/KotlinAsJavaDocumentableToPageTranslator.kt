package org.jetbrains.dokka.kotlinAsJava.translators

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinAsJavaDocumentableToPageTranslator(
    private val commentsToContentConverter: CommentsToContentConverter,
    private val signatureProvider: SignatureProvider,
    private val logger: DokkaLogger
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): ModulePageNode =
        KotlinAsJavaPageCreator(commentsToContentConverter, signatureProvider, logger).pageForModule(module)
}