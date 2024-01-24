/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.allModulesPage.MultimodulePageCreator.Companion.MULTIMODULE_PACKAGE_PLACEHOLDER
import org.jetbrains.dokka.base.resolvers.local.DokkaBaseLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

public open class MultimoduleLocationProvider(
    private val root: RootPageNode, dokkaContext: DokkaContext,
    public val extension: String = ".html"
) : DokkaBaseLocationProvider(root, dokkaContext) {

    private val defaultLocationProvider =
        dokkaContext.plugin<AllModulesPagePlugin>().querySingle { partialLocationProviderFactory }
            .getLocationProvider(root)
    private val externalModuleLinkResolver =
        dokkaContext.plugin<AllModulesPagePlugin>().querySingle { externalModuleLinkResolver }

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?): String? {
        return if (dri == MultimodulePageCreator.MULTIMODULE_ROOT_DRI) {
            pathToRoot(root) + "index"
        } else {
            dri.takeIf { it.packageName == MULTIMODULE_PACKAGE_PLACEHOLDER }
                ?.classNames
                ?.let(externalModuleLinkResolver::resolveLinkToModuleIndex)
        }
    }

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String? {
        return if (node is ContentPage && MultimodulePageCreator.MULTIMODULE_ROOT_DRI in node.dri) {
            pathToRoot(root) + "index" + if (!skipExtension) extension else ""
        } else {
            defaultLocationProvider.resolve(node, context, skipExtension)
        }
    }

    override fun pathToRoot(from: PageNode): String = defaultLocationProvider.pathToRoot(from)

    override fun ancestors(node: PageNode): List<PageNode> = listOf(root)

    public class Factory(
        private val context: DokkaContext
    ) : LocationProviderFactory {
        override fun getLocationProvider(pageNode: RootPageNode): LocationProvider =
            MultimoduleLocationProvider(pageNode, context)
    }
}
