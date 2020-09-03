package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.location.JavadocLocationProviderFactory
import org.jetbrains.dokka.javadoc.renderer.KorteJavadocRenderer
import org.jetbrains.dokka.javadoc.signatures.JavadocSignatureProvider
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.kotlinAsJava.KotlinAsJavaPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer

class JavadocPlugin : DokkaPlugin() {

    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }
    val kotinAsJavaPlugin by lazy { plugin<KotlinAsJavaPlugin>() }
    val locationProviderFactory by lazy { dokkaBasePlugin.locationProviderFactory }
    val javadocPreprocessors by extensionPoint<PageTransformer>()

    val dokkaJavadocPlugin by extending {
        (CoreExtensions.renderer
                providing { ctx -> KorteJavadocRenderer(dokkaBasePlugin.querySingle { outputWriter }, ctx, "views") }
                override dokkaBasePlugin.htmlRenderer)
    }

    val pageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing { context ->
            JavadocDocumentableToPageTranslator(
                context,
                dokkaBasePlugin.querySingle { signatureProvider },
                context.logger
            )
        } override dokkaBasePlugin.documentableToPageTranslator
    }

    val javadocLocationProviderFactory by extending {
        dokkaBasePlugin.locationProviderFactory providing { context ->
            JavadocLocationProviderFactory(context)
        } override dokkaBasePlugin.locationProvider
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

    val rootCreator by extending {
        javadocPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        javadocPreprocessors providing {
            PackageListCreator(
                context = it,
                format = RecognizedLinkFormat.DokkaJavadoc,
                outputFilesNames = listOf("package-list", "element-list")
            )
        } order { after(rootCreator) }
    }

    val resourcesInstaller by extending {
        javadocPreprocessors with ResourcesInstaller order { after(rootCreator) }
    }

    val treeViewInstaller by extending {
        javadocPreprocessors with TreeViewInstaller order { after(rootCreator) }
    }

    val allClassessPageInstaller by extending {
        javadocPreprocessors with AllClassesPageInstaller order { before(rootCreator) }
    }

    val indexGenerator by extending {
        javadocPreprocessors with IndexGenerator order { before(rootCreator) }
    }

    val deprecatedPageCreator by extending {
        javadocPreprocessors with DeprecatedPageCreator order { before(rootCreator) }
    }
}

