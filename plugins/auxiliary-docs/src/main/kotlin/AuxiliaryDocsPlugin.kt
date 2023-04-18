package org.jetbrains.dokka.auxiliaryDocs

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.pages.PageTransformer

class AuxiliaryDocsPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer providing {
            AuxDocsTransformer(it)
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

class AuxDocsTransformer(private val context: DokkaContext) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        val conf = pluginConfiguration()

        val creator = AuxiliaryDocPageCreator(context)
        val homePage = creator.root(AuxiliaryDocPageContext("Home", conf))

        return homePage.modified(
            children = creator.samplePages(conf).toList() + input
        )
    }

    private fun AuxiliaryDocPageCreator.samplePages(conf: AuxiliaryConfiguration): Set<PageNode> {
        //todo
        val quickStart = page(AuxiliaryDocPageContext("Quick Start", conf))
        val faqStart = page(AuxiliaryDocPageContext("F.A.Q", conf))

        return setOf(quickStart, faqStart)
    }

    // todo: handle empty configuration
    private fun pluginConfiguration() =
        parseJson<AuxiliaryConfiguration>(
            context.configuration.pluginsConfiguration
                .first { it.fqPluginName == "org.jetbrains.dokka.auxiliaryDocs.AuxiliaryDocsPlugin" }.values
        )
}



