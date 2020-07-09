package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureProvider
import org.jetbrains.dokka.kotlinAsJava.transformers.KotlinAsJavaDocumentableTransformer
import org.jetbrains.dokka.plugability.DokkaPlugin

class KotlinAsJavaPlugin : DokkaPlugin() {
    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer applyIf { format == JAVADOC_FORMAT }
    }
    val kotlinAsJavaDocumentableToPageTranslator by extending {
        (CoreExtensions.documentableTransformer
                with KotlinAsJavaDocumentableTransformer()
                applyIf { format == JAVADOC_FORMAT })
    }
    val javaSignatureProvider by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        dokkaBasePlugin.signatureProvider providing { ctx ->
            JavaSignatureProvider(ctx.single(dokkaBasePlugin.commentsToContentConverter), ctx.logger)
        } override dokkaBasePlugin.kotlinSignatureProvider applyIf { format == JAVADOC_FORMAT }
    }

    companion object {
        const val JAVADOC_FORMAT = "javadoc"
    }
}
