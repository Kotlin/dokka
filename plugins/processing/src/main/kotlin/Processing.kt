package org.jetbrains.dokka.processing

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.processing.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.processing.signatures.SignatureProvider
import org.jetbrains.dokka.processing.transformers.documentables.ExtensionExtractorTransformer
import org.jetbrains.dokka.processing.transformers.documentables.InheritorsExtractorTransformer
import org.jetbrains.dokka.processing.translators.docTags.CommentsToContentTranslator
import org.jetbrains.dokka.processing.translators.docTags.DocTagToContentTranslator
import org.jetbrains.dokka.processing.translators.documentables.DefaultDocumentableToPageTranslator

class Processing: DokkaPlugin() {
    val commentsToContentTranslator by extensionPoint<CommentsToContentTranslator>()
    val signatureProvider by extensionPoint<SignatureProvider>()

    val docTagToContentConverter by extending {
        commentsToContentTranslator with DocTagToContentTranslator
    }

    val kotlinSignatureProvider by extending {
            signatureProvider providing { ctx ->
                KotlinSignatureProvider(ctx.single(commentsToContentTranslator), ctx.logger)
            }
        }

    val documentableToPageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing { ctx ->
            DefaultDocumentableToPageTranslator(
                ctx.single(commentsToContentTranslator),
                ctx.single(signatureProvider),
                ctx.logger
            )
        }
    }

    val inheritorsExtractor by extending {
        CoreExtensions.documentableTransformer with InheritorsExtractorTransformer()
    }

    val extensionsExtractor by extending {
        CoreExtensions.documentableTransformer with ExtensionExtractorTransformer()
    }

}