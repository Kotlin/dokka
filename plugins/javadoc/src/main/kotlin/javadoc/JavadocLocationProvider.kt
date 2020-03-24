package javadoc

import org.jetbrains.dokka.base.resolvers.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.PlatformData

class JavadocLocationProvider : LocationProvider {
    override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String =
        dri.packageName + if (dri.classNames != null)
            "/${dri.classNames}"
        else
            "/package-summary"

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String {
        TODO("Not yet implemented")
    }

    override fun resolveRoot(node: PageNode): String {
        TODO("Not yet implemented")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        TODO("Not yet implemented")
    }
}