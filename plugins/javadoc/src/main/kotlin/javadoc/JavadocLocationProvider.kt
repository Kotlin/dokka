package javadoc

import javadoc.pages.AllClassesPage
import javadoc.pages.JavadocClasslikePageNode
import javadoc.pages.JavadocModulePageNode
import javadoc.pages.JavadocPackagePageNode
import org.jetbrains.dokka.base.resolvers.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.PlatformData

class JavadocLocationProvider : LocationProvider {
    override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String = if (context != null)
            resolve(context, skipExtension = true)
        else if (dri.packageName != null) {
        "${dri.packageName}/${dri.classNames ?: "package-summary"}"
    }
    else {
        "allclasses"
    }

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String = when(node) {
        is JavadocModulePageNode -> "index"
        is JavadocPackagePageNode -> "${node.name}/package-summary"
        is JavadocClasslikePageNode -> "${node.dri.first().packageName}/${node.dri.first().classNames}"
        is AllClassesPage -> "allclasses"
        else -> ""
    } + ".html".takeUnless { skipExtension }.orEmpty()

    override fun resolveRoot(node: PageNode): String {
        TODO("Not yet implemented")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        TODO("Not yet implemented")
    }
}