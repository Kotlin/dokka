/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinPlaygroundSamples

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class KotlinPlaygroundSamplesPlugin : DokkaPlugin() {

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    public val kotlinPlaygroundSamplesTransformer: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing ::KotlinPlaygroundSamplesTransformer override dokkaBase.defaultSamplesTransformer
    }

    public val kotlinPlaygroundSamplesScriptsInstaller: Extension<PageTransformer, *, *> by extending {
        dokkaBase.htmlPreprocessors providing ::KotlinPlaygroundSamplesScriptsInstaller order {
            after(dokkaBase.scriptsInstaller)
            before(dokkaBase.customResourceInstaller)
        }
    }

    public val kotlinPlaygroundSamplesStylesInstaller: Extension<PageTransformer, *, *> by extending {
        dokkaBase.htmlPreprocessors providing ::KotlinPlaygroundSamplesStylesInstaller order {
            after(dokkaBase.stylesInstaller)
            before(dokkaBase.customResourceInstaller)
        }
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
