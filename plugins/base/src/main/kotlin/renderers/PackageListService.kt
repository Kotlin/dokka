package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class PackageListService(val context: DokkaContext, val rootPage: RootPageNode) {

    fun createPackageList(module: ModulePage, format: String, linkExtension: String): String {

        val packages = mutableSetOf<String>()
        val nonStandardLocations = mutableMapOf<String, String>()

        val locationProvider =
            context.plugin<DokkaBase>().querySingle { locationProviderFactory }.getLocationProvider(rootPage)

        fun visit(node: PageNode) {
            if (node is PackagePage) {
                node.name
                    .takeUnless { name -> name.startsWith("[") && name.endsWith("]") } // Do not include the package name for declarations without one
                    ?.let { packages.add(it) }
            }

            val contentPage = node.safeAs<ContentPage>()
            contentPage?.dri?.forEach { dri ->
                val nodeLocation = locationProvider.resolve(node, context = module, skipExtension = true)
                    ?: run { context.logger.error("Cannot resolve path for ${node.name}!"); null }

                if (dri != DRI.topLevel && locationProvider.expectedLocationForDri(dri) != nodeLocation) {
                    nonStandardLocations[dri.toString()] = "$nodeLocation.$linkExtension"
                }
            }

            node.children.forEach { visit(it) }
        }

        visit(module)

        return buildString {
            appendLine("$DOKKA_PARAM_PREFIX.format:${format}")
            appendLine("$DOKKA_PARAM_PREFIX.linkExtension:${linkExtension}")
            nonStandardLocations.map { (signature, location) -> "$DOKKA_PARAM_PREFIX.location:$signature\u001f$location" }
                .sorted().joinTo(this, separator = "\n", postfix = "\n")

            packages.sorted().joinTo(this, separator = "\n", postfix = "\n")
        }

    }

    companion object {
        const val DOKKA_PARAM_PREFIX = "\$dokka"
    }
}
