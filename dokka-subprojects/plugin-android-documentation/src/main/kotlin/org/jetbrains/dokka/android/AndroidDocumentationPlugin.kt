/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.android

import org.jetbrains.dokka.android.transformers.HideTagDocumentableFilter
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

public class AndroidDocumentationPlugin : DokkaPlugin() {

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    public val suppressedByHideTagDocumentableFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        dokkaBase.preMergeDocumentableTransformer providing ::HideTagDocumentableFilter order { before(dokkaBase.emptyPackagesFilter) }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
