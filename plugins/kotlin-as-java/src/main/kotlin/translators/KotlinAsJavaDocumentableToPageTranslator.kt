package org.jetbrains.dokka.kotlinAsJava.translators

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator

class KotlinAsJavaDocumentableToPageTranslator(
    private val commentsToContentConverter: CommentsToContentConverter,
    private val signatureProvider: SignatureProvider,
    private val context: DokkaContext
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): ModulePageNode =
        KotlinAsJavaPageCreator(commentsToContentConverter, signatureProvider, context).pageForModule(module)
}
