package javadoc

import javadoc.pages.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.resolvers.ExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext

class JavadocLocationProvider(private val context: DokkaContext) : LocationProvider {
    private val externalLocationProvider = ExternalLocationProvider

    override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String = if (context != null)
        resolve(context, skipExtension = true)
    else if (dri.packageName != null) {
        "${dri.packageName}/${dri.classNames ?: "package-summary"}"
    } else {
        "allclasses"
    }

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String = when (node) {
        is JavadocModulePageNode -> "index"
        is JavadocPackagePageNode -> "${node.name}/package-summary"
        is JavadocClasslikePageNode -> "${node.dri.first().packageName}/${node.dri.first().classNames}"
        is TreeViewPage -> when (node.documentable) {
            is DPackage -> "${node.documentable.name}/package-tree"
            else -> "overview-tree"
        }
        is AllClassesPage -> "allclasses"
        is ContentNode -> node.dci.dri.firstOrNull()?.let { dri ->
            externalLocationProvider.getLocation(dri,
                this.context.configuration.passesConfigurations.filter { it.analysisPlatform == Platform.jvm }
                    .flatMap { it.externalDocumentationLinks })
        } ?: run { throw IllegalStateException("Location for ${node.name} could not be resolved") }
        else -> throw IllegalStateException("Location for ${node.name} could not be resolved")
    } + ".html".takeUnless { skipExtension }.orEmpty()

    override fun resolveRoot(node: PageNode): String {
        TODO("Not yet implemented")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        TODO("Not yet implemented")
    }
}