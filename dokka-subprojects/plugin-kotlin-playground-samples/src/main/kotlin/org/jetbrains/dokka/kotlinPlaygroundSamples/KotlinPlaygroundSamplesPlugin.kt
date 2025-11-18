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

    public val kotlinPlaygroundSamplesTransformer: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing ::KotlinPlaygroundSamplesTransformer override plugin<DokkaBase>().defaultSamplesTransformer
    }

    public val kotlinPlaygroundSamplesScriptsInstaller: Extension<PageTransformer, *, *> by extending {
        plugin<DokkaBase>().htmlPreprocessors providing ::KotlinPlaygroundSamplesScriptsInstaller order {
            after(plugin<DokkaBase>().scriptsInstaller)
            before(plugin<DokkaBase>().customResourceInstaller)
        }
    }

    public val kotlinPlaygroundSamplesStylesInstaller: Extension<PageTransformer, *, *> by extending {
        plugin<DokkaBase>().htmlPreprocessors providing ::KotlinPlaygroundSamplesStylesInstaller order {
            after(plugin<DokkaBase>().stylesInstaller)
            before(plugin<DokkaBase>().customResourceInstaller)
        }
    }

    public companion object {
        public const val FQN: String = "org.jetbrains.dokka.kotlinPlaygroundSamples.KotlinPlaygroundSamplesPlugin"
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
