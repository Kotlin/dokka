package org.jetbrains.dokka.javadoc

import javadoc.JavadocDocumentableToPageTranslator
import javadoc.location.JavadocLocationProviderFactory
import javadoc.renderer.KorteJavadocRenderer
import javadoc.signatures.JavadocSignatureProvider
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.kotlinAsJava.KotlinAsJavaPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle

class JavadocPlugin : DokkaPlugin() {

    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }
    val kotinAsJavaPlugin by lazy { plugin<KotlinAsJavaPlugin>() }

    val locationProviderFactory by extensionPoint<JavadocLocationProviderFactory>()

    val dokkaJavadocPlugin by extending {
        (CoreExtensions.renderer
                providing { ctx -> KorteJavadocRenderer(dokkaBasePlugin.querySingle { outputWriter }, ctx, "views") }
                override dokkaBasePlugin.htmlRenderer)
    }

    val pageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing { context ->
            JavadocDocumentableToPageTranslator(
                dokkaBasePlugin.querySingle { commentsToContentConverter },
                dokkaBasePlugin.querySingle { signatureProvider },
                context.logger
            )
        } override dokkaBasePlugin.documentableToPageTranslator
    }

    val javadocLocationProviderFactory by extending {
        locationProviderFactory providing { context ->
            JavadocLocationProviderFactory(context)
        }
    }

    val javadocSignatureProvider by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        dokkaBasePlugin.signatureProvider providing { ctx ->
            JavadocSignatureProvider(
                ctx.single(
                    dokkaBasePlugin.commentsToContentConverter
                ), ctx.logger
            )
        } override kotinAsJavaPlugin.javaSignatureProvider
    }
}

