/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.javadoc.pages.JavadocSignatureContentNode
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.utilities.DokkaLogger

public class JavadocPageContentBuilder(
    commentsConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    logger: DokkaLogger
) : PageContentBuilder(commentsConverter, signatureProvider, logger) {

    public fun PageContentBuilder.DocumentableContentBuilder.javadocGroup(
        dri: DRI = mainDRI.first(),
        sourceSets: Set<DokkaConfiguration.DokkaSourceSet> = mainSourcesetData,
        extra: PropertyContainer<ContentNode> = mainExtra,
        block: JavadocContentBuilder.() -> Unit
    ) {
        +JavadocContentBuilder(
            mainDri = dri,
            mainExtra = extra,
            mainSourceSet = sourceSets,
        ).apply(block).build()
    }

    public open inner class JavadocContentBuilder(
        private val mainDri: DRI,
        private val mainExtra: PropertyContainer<ContentNode>,
        private val mainSourceSet: Set<DokkaConfiguration.DokkaSourceSet>,
    ) {
        public var annotations: ContentNode? = null
        public var modifiers: ContentNode? = null
        public var signatureWithoutModifiers: ContentNode? = null
        public var supertypes: ContentNode? = null

        public fun annotations(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            val built = buildContentForBlock(block)
            if(built.hasAnyContent()) annotations = built
        }

        public fun modifiers(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            val built = buildContentForBlock(block)
            if(built.hasAnyContent()) modifiers = built
        }

        public fun signatureWithoutModifiers(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            signatureWithoutModifiers = buildContentForBlock(block)
        }

        public fun supertypes(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) {
            val built = buildContentForBlock(block)
            if(built.hasAnyContent()) supertypes = built
        }

        private fun buildContentForBlock(block: PageContentBuilder.DocumentableContentBuilder.() -> Unit) =
            contentFor(
                dri = mainDri,
                sourceSets = mainSourceSet,
                kind = ContentKind.Symbol,
                extra = mainExtra,
                block = block
            )

        public fun build(): JavadocSignatureContentNode = JavadocSignatureContentNode(
            dri = mainDri,
            annotations = annotations,
            modifiers = modifiers,
            signatureWithoutModifiers = signatureWithoutModifiers ?: throw IllegalStateException("JavadocSignatureContentNode should have at least a signature"),
            supertypes = supertypes
        )
    }
}
