package org.jetbrains.dokka.kotlinAsJava.translators

import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.base.translators.documentables.DefaultPageCreator
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.analysis.kotlin.internal.DocumentableSourceLanguageParser

class KotlinAsJavaPageCreator(
    configuration: DokkaBaseConfiguration?,
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    logger: DokkaLogger,
    customTagContentProviders: List<CustomTagContentProvider>,
    documentableAnalyzer: DocumentableSourceLanguageParser
) : DefaultPageCreator(
    configuration,
    commentsToContentConverter,
    signatureProvider,
    logger,
    customTagContentProviders = customTagContentProviders,
    documentableAnalyzer = documentableAnalyzer
) {
    override fun pageForProperty(p: DProperty): MemberPageNode? = null
}
