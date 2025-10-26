/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.runnablesamples

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class RunnableSamplesPlugin : DokkaPlugin() {

    public val runnableSamplesTransformer: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing ::RunnableSamplesTransformer override plugin<DokkaBase>().defaultSamplesTransformer
    }

    public val runnableSamplesScriptsInstaller: Extension<PageTransformer, *, *> by extending {
        plugin<DokkaBase>().htmlPreprocessors providing ::RunnableSamplesScriptsInstaller order {
            after(plugin<DokkaBase>().scriptsInstaller)
            before(plugin<DokkaBase>().customResourceInstaller)
        }
    }

    public val runnableSamplesStylesInstaller: Extension<PageTransformer, *, *> by extending {
        plugin<DokkaBase>().htmlPreprocessors providing ::RunnableSamplesStylesInstaller order {
            after(plugin<DokkaBase>().stylesInstaller)
            before(plugin<DokkaBase>().customResourceInstaller)
        }
    }

    public companion object {
        public const val FQN: String = "org.jetbrains.dokka.runnablesamples.RunnableSamplesPlugin"
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
