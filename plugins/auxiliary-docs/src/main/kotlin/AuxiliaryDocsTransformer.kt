package org.jetbrains.dokka.auxiliaryDocs

import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer


private const val AUXDOCS_PLUGIN_FQN = "org.jetbrains.dokka.auxiliaryDocs.AuxiliaryDocsPlugin"

class AuxiliaryDocsTransformer(private val context: DokkaContext) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        context.logger.progress("Configuring Auxiliary Documentation pages")
        val configuration: AuxiliaryConfiguration = pluginConfiguration()

        val newRoot = AuxiliaryDocPageCreator(context).invoke(createContext(configuration))
        return newRoot.modified(
            children = newRoot.children + input.transformRootNodeToUsual(configuration.apiReferenceNodeName)
        )
    }

    private fun createContext(configuration: AuxiliaryConfiguration): AuxiliaryDocPageContext {
        val rootPageFile = configuration.entryPointNode
            ?: throw Exception("Auxiliary Documentation plugin is applied but no entry point file passed")

        val contentPageFiles = configuration.nodesDir?.listFiles()
            ?.filter { it != rootPageFile }
            ?.toSet() ?: emptySet()

        return AuxiliaryDocPageContext(
            rootPage = rootPageFile,
            contentPages = contentPageFiles
        )
    }

    private fun RootPageNode.transformRootNodeToUsual(apiReferenceName: String?): PageNode {
        if (this !is ModulePageNode) return this
        return AuxiliaryPageNode(
            name = apiReferenceName ?: name,
            dri = setOf(DRI(classNames = apiReferenceName ?: name)),
            content = content,
            embeddedResources = embeddedResources,
            children = children
        )
    }

    // todo: handle empty configuration
    private fun pluginConfiguration() =
        parseJson<AuxiliaryConfiguration>(
            context.configuration.pluginsConfiguration
                .first { it.fqPluginName == AUXDOCS_PLUGIN_FQN }.values
        )
}
