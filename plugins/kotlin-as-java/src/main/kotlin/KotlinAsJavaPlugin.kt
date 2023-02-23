package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureProvider
import org.jetbrains.dokka.kotlinAsJava.transformers.JvmNameDocumentableTransformer
import org.jetbrains.dokka.kotlinAsJava.transformers.KotlinAsJavaDocumentableTransformer
import org.jetbrains.dokka.kotlinAsJava.translators.KotlinAsJavaDocumentableToPageTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.renderers.PostAction

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaDocumentableTransformer by extending {
        CoreExtensions.documentableTransformer with KotlinAsJavaDocumentableTransformer()
    }

    val jvmNameTransformer by extending {
        CoreExtensions.documentableTransformer with JvmNameDocumentableTransformer() order {
            after(kotlinAsJavaDocumentableTransformer)
        }
    }

    val javaSignatureProvider by extending {
        with(plugin<DokkaBase>()) {
            signatureProvider providing ::JavaSignatureProvider override kotlinSignatureProvider
        }
    }

    val kotlinAsJavaDocumentableToPageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing ::KotlinAsJavaDocumentableToPageTranslator override
                plugin<DokkaBase>().documentableToPageTranslator
    }

    internal val alphaVersionNotifier by extending {
        CoreExtensions.postActions providing { ctx ->
            PostAction {
                ctx.logger.info("KotlinAsJava plugin is in Alpha version, use at your own risk, expect bugs and migration issues")
            }
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
