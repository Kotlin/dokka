package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.allModulesPage.MultimodulePageCreator.Companion.MULTIMODULE_PACKAGE_PLACEHOLDER
import org.jetbrains.dokka.base.resolvers.local.DokkaBaseLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.OutputExtension
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

open class MultimoduleLocationProvider(private val root: RootPageNode, dokkaContext: DokkaContext, private val outputExtension: OutputExtension) :
    DokkaBaseLocationProvider(root, dokkaContext) {

    private val defaultLocationProvider =
        dokkaContext.plugin<AllModulesPagePlugin>().querySingle { partialLocationProviderFactory }
            .getLocationProvider(root, outputExtension)
    private val externalModuleLinkResolver =
        dokkaContext.plugin<AllModulesPagePlugin>().querySingle { externalModuleLinkResolver }

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?) =
        if (dri == MultimodulePageCreator.MULTIMODULE_ROOT_DRI) pathToRoot(root) + "index$outputExtension"
        else dri.takeIf { it.packageName == MULTIMODULE_PACKAGE_PLACEHOLDER }?.classNames
            ?.let(externalModuleLinkResolver::resolveLinkToModuleIndex)

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean) =
        if (node is ContentPage && MultimodulePageCreator.MULTIMODULE_ROOT_DRI in node.dri) pathToRoot(root) + "index$outputExtension"
        else defaultLocationProvider.resolve(node, context, skipExtension)

    override fun pathToRoot(from: PageNode): String =
        if (from is ContentPage && MultimodulePageCreator.MULTIMODULE_ROOT_DRI in from.dri) ""
        else defaultLocationProvider.pathToRoot(from)

    override fun ancestors(node: PageNode): List<PageNode> = listOf(root)

    class Factory(private val context: DokkaContext) : LocationProviderFactory {
        override fun getLocationProvider(pageNode: RootPageNode, outputExtension: OutputExtension) =
            MultimoduleLocationProvider(pageNode, context, "." + outputExtension.removePrefix("."))
    }
}
