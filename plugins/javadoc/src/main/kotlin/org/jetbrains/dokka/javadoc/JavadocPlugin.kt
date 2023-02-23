package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.location.JavadocLocationProviderFactory
import org.jetbrains.dokka.javadoc.renderer.KorteJavadocRenderer
import org.jetbrains.dokka.javadoc.signatures.JavadocSignatureProvider
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.shared.PackageList.Companion.PACKAGE_LIST_NAME
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.javadoc.transformers.documentables.JavadocDocumentableJVMSourceSetFilter
import org.jetbrains.dokka.javadoc.validity.MultiplatformConfiguredChecker
import org.jetbrains.dokka.kotlinAsJava.KotlinAsJavaPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.transformers.pages.PageTransformer

class JavadocPlugin : DokkaPlugin() {

    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }
    val kotinAsJavaPlugin by lazy { plugin<KotlinAsJavaPlugin>() }
    val locationProviderFactory by lazy { dokkaBasePlugin.locationProviderFactory }
    val javadocPreprocessors by extensionPoint<PageTransformer>()

    val dokkaJavadocPlugin by extending {
        CoreExtensions.renderer providing { ctx -> KorteJavadocRenderer(ctx, "views") } override dokkaBasePlugin.htmlRenderer
    }

    val javadocMultiplatformCheck by extending {
        CoreExtensions.preGenerationCheck providing ::MultiplatformConfiguredChecker
    }

    val pageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing ::JavadocDocumentableToPageTranslator override
                kotinAsJavaPlugin.kotlinAsJavaDocumentableToPageTranslator
    }

    val documentableSourceSetFilter by extending {
        dokkaBasePlugin.preMergeDocumentableTransformer providing ::JavadocDocumentableJVMSourceSetFilter
    }

    val javadocLocationProviderFactory by extending {
        dokkaBasePlugin.locationProviderFactory providing ::JavadocLocationProviderFactory override dokkaBasePlugin.locationProvider
    }

    val javadocSignatureProvider by extending {
        dokkaBasePlugin.signatureProvider providing ::JavadocSignatureProvider override kotinAsJavaPlugin.javaSignatureProvider
    }

    val rootCreator by extending {
        javadocPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        javadocPreprocessors providing {
            PackageListCreator(
                context = it,
                format = RecognizedLinkFormat.DokkaJavadoc,
                outputFilesNames = listOf(PACKAGE_LIST_NAME, "element-list")
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

    internal val alphaVersionNotifier by extending {
        CoreExtensions.postActions providing { ctx ->
            PostAction {
                ctx.logger.info(
                    "The Javadoc output format is still in Alpha so you may find bugs and experience migration " +
                            "issues when using it. Successful integration with tools that accept Java's Javadoc " +
                            "HTML as input is not guaranteed. You use it at your own risk."
                )
            }
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

