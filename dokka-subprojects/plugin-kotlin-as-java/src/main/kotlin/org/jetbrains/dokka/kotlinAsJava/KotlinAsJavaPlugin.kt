/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureProvider
import org.jetbrains.dokka.kotlinAsJava.transformers.JvmNameDocumentableTransformer
import org.jetbrains.dokka.kotlinAsJava.transformers.KotlinAsJavaDocumentableTransformer
import org.jetbrains.dokka.kotlinAsJava.translators.KotlinAsJavaDocumentableToPageTranslator
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

public class KotlinAsJavaPlugin : DokkaPlugin() {
    private val dokkaBasePlugin: DokkaBase by lazy { plugin<DokkaBase>() }

    public val suppressJvmMappedMethodsFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        dokkaBasePlugin.preMergeDocumentableTransformer providing {
            object : PreMergeDocumentableTransformer {
                override fun invoke(modules: List<DModule>) = modules
            }
        } override dokkaBasePlugin.jvmMappedMethodsFilter
    }

    public val kotlinAsJavaDocumentableTransformer: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with KotlinAsJavaDocumentableTransformer()
    }

    public val jvmNameTransformer: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with JvmNameDocumentableTransformer() order {
            after(kotlinAsJavaDocumentableTransformer)
        }
    }

    public val javaSignatureProvider: Extension<SignatureProvider, *, *> by extending {
        with(plugin<DokkaBase>()) {
            signatureProvider providing ::JavaSignatureProvider override kotlinSignatureProvider
        }
    }

    public val kotlinAsJavaDocumentableToPageTranslator: Extension<DocumentableToPageTranslator, *, *> by extending {
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
