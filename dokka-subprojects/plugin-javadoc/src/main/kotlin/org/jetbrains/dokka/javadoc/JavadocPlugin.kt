/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.PackageList.Companion.PACKAGE_LIST_NAME
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.javadoc.location.JavadocLocationProviderFactory
import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.javadoc.renderer.KorteJavadocRenderer
import org.jetbrains.dokka.javadoc.signatures.JavadocSignatureProvider
import org.jetbrains.dokka.javadoc.transformers.documentables.JavadocDocumentableJVMSourceSetFilter
import org.jetbrains.dokka.javadoc.validity.MultiplatformConfiguredChecker
import org.jetbrains.dokka.kotlinAsJava.KotlinAsJavaPlugin
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.validity.PreGenerationChecker

public class JavadocPlugin : DokkaPlugin() {

    private val dokkaBasePlugin: DokkaBase by lazy { plugin<DokkaBase>() }
    private val kotinAsJavaPlugin: KotlinAsJavaPlugin by lazy { plugin<KotlinAsJavaPlugin>() }

    public val locationProviderFactory: ExtensionPoint<LocationProviderFactory> by lazy { dokkaBasePlugin.locationProviderFactory }
    public val javadocPreprocessors: ExtensionPoint<PageTransformer> by extensionPoint<PageTransformer>()

    public val dokkaJavadocPlugin: Extension<Renderer, *, *> by extending {
        CoreExtensions.renderer providing { ctx -> KorteJavadocRenderer(ctx, "views") } override dokkaBasePlugin.htmlRenderer
    }

    public val javadocMultiplatformCheck: Extension<PreGenerationChecker, *, *> by extending {
        CoreExtensions.preGenerationCheck providing ::MultiplatformConfiguredChecker
    }

    public val pageTranslator: Extension<DocumentableToPageTranslator, *, *> by extending {
        CoreExtensions.documentableToPageTranslator providing ::JavadocDocumentableToPageTranslator override
                kotinAsJavaPlugin.kotlinAsJavaDocumentableToPageTranslator
    }

    public val documentableSourceSetFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        dokkaBasePlugin.preMergeDocumentableTransformer providing ::JavadocDocumentableJVMSourceSetFilter
    }

    public val javadocLocationProviderFactory: Extension<LocationProviderFactory, *, *> by extending {
        dokkaBasePlugin.locationProviderFactory providing ::JavadocLocationProviderFactory override dokkaBasePlugin.locationProvider
    }

    public val javadocSignatureProvider: Extension<SignatureProvider, *, *> by extending {
        dokkaBasePlugin.signatureProvider providing ::JavadocSignatureProvider override kotinAsJavaPlugin.javaSignatureProvider
    }

    public val rootCreator: Extension<PageTransformer, *, *> by extending {
        javadocPreprocessors with RootCreator
    }

    public val packageListCreator: Extension<PageTransformer, *, *> by extending {
        javadocPreprocessors providing {
            PackageListCreator(
                context = it,
                format = RecognizedLinkFormat.DokkaJavadoc,
                outputFilesNames = listOf(PACKAGE_LIST_NAME, "element-list")
            )
        } order { after(rootCreator) }
    }

    public val resourcesInstaller: Extension<PageTransformer, *, *> by extending {
        javadocPreprocessors with ResourcesInstaller order { after(rootCreator) }
    }

    public val treeViewInstaller: Extension<PageTransformer, *, *> by extending {
        javadocPreprocessors providing ::TreeViewInstaller order { after(rootCreator) }
    }

    public val allClassessPageInstaller: Extension<PageTransformer, *, *> by extending {
        javadocPreprocessors with AllClassesPageInstaller order { before(rootCreator) }
    }

    public val indexGenerator: Extension<PageTransformer, *, *> by extending {
        javadocPreprocessors with IndexGenerator order { before(rootCreator) }
    }

    public val deprecatedPageCreator: Extension<PageTransformer, *, *> by extending {
        javadocPreprocessors with DeprecatedPageCreator order { before(rootCreator) }
    }

    // defaultSamplesTransformer knows nothing about Javadoc's content model
    internal val emptySampleTransformer: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing {
            PageTransformer { it }
        } override dokkaBasePlugin.defaultSamplesTransformer
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

