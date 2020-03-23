package javadoc.pages

import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificPage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode

internal const val jQueryVersion = "3.3.1"
internal const val jQueryMigrateVersion = "3.0.1"

class RootIndexPage(override val name: String, override val children: List<PageNode>, val root: RootPageNode) :
    RootPageNode(), RendererSpecificPage {
    override val strategy: RenderingStrategy = RenderingStrategy.Callback { content() }
    val version: String = "0.0.1"
    val pathToRoot: String = ""

    override fun modified(name: String, children: List<PageNode>): RootPageNode = RootIndexPage(name, children, root)

    private fun content() =
        pageStart(name, version, name, pathToRoot) +
                topNavbar(root, pathToRoot) +
                indexPage(
                    name,
                    version,
                    "Packages",
                    "Package",
                    children.filterIsInstance<JavadocPageNode>().filter { it.pageType == PageType.Package })

}

//class PackageSummary(val page: PageNode) : RendererSpecificPage {
//    override val name = "package-summary"
//    override val children = emptyList<PageNode>()
//    override fun modified(name: String, children: List<PageNode>) = this
//
//    override val strategy = RenderingStrategy.Write(content())
//
//    private fun content(): String = pageStart(page.name, "0.0.1", page.name, "../") + // TODO
//            topNavbar(page, "???")
//
//}

