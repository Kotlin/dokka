package org.jetbrains.dokka.kotlinAsJava.translators

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.DefaultPageCreator
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinAsJavaPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    logger: DokkaLogger
) : DefaultPageCreator(commentsToContentConverter, signatureProvider, logger) {
    override fun pageForProperty(p: DProperty): MemberPageNode? = null
}