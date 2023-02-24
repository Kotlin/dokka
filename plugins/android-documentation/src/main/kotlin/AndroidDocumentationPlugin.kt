package org.jetbrains.dokka.android

import org.jetbrains.dokka.android.transformers.HideTagDocumentableFilter
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.PreviewDokkaPluginApi

class AndroidDocumentationPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val suppressedByHideTagDocumentableFilter by extending {
        dokkaBase.preMergeDocumentableTransformer providing ::HideTagDocumentableFilter order { before(dokkaBase.emptyPackagesFilter) }
    }

    @PreviewDokkaPluginApi
    override fun previewDokkaPluginApiEmptyMethod() {
    }
}