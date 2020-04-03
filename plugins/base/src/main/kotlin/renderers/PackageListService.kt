package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class PackageListService(val context: DokkaContext) {

    fun formatPackageList(module: RootPageNode, format: String, linkExtension: String): String {

        val packages = mutableSetOf<String>()
        val nonStandardLocations = mutableMapOf<String, String>()

        val locationProvider =
            context.plugin<DokkaBase>().querySingle { locationProviderFactory }.getLocationProvider(module)

        fun visit(node: PageNode, parentDris: Set<DRI>) {

            if (node is PackagePageNode) {
                packages.add(node.name)
            }

            val contentPage = node.safeAs<ContentPage>()
            contentPage?.dri?.forEach {
                if (parentDris.isNotEmpty() && it.parent !in parentDris) {
                    nonStandardLocations[it.toString()] = locationProvider.resolve(node)
                }
            }

            node.children.forEach { visit(it, contentPage?.dri ?: setOf()) }
        }

        visit(module, setOf())

        return buildString {
            appendln("\$dokka.format:${format}")
            appendln("\$dokka.linkExtension:${linkExtension}")
            nonStandardLocations.map { (signature, location) -> "\$dokka.location:$signature\u001f$location" }
                .sorted().joinTo(this, separator = "\n", postfix = "\n")

            packages.sorted().joinTo(this, separator = "\n", postfix = "\n")
        }

    }

}