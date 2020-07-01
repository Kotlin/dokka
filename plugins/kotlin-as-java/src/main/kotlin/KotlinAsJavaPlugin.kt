package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureProvider
import org.jetbrains.dokka.kotlinAsJava.transformers.KotlinAsJavaDocumentableTransformer
import org.jetbrains.dokka.plugability.DokkaPlugin

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaDocumentableToPageTranslator by extending {
        CoreExtensions.documentableTransformer with KotlinAsJavaDocumentableTransformer()
    }
    val javaSignatureProvider by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        dokkaBasePlugin.signatureProvider providing { ctx ->
            JavaSignatureProvider(ctx.single(dokkaBasePlugin.commentsToContentConverter), ctx.logger)
        } override dokkaBasePlugin.kotlinSignatureProvider
    }
}