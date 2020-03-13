package org.jetbrains.dokka.javadoc

import javadoc.JavadocDocumentableToPageTranslator
import javadoc.KorteJavadocRenderer
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle

class JavadocPlugin : DokkaPlugin() {
    val dokkaJavadocPlugin by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        CoreExtensions.renderer providing { ctx ->
            KorteJavadocRenderer(dokkaBasePlugin.querySingle { outputWriter }, ctx, "views")
        }
    }

    val pageTranslator by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        CoreExtensions.documentableToPageTranslator providing { ctx ->
            JavadocDocumentableToPageTranslator(
                dokkaBasePlugin.querySingle { commentsToContentConverter },
                dokkaBasePlugin.querySingle { signatureProvider },
                ctx.logger
            )
        }
    }
}

