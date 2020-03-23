package javadoc.pages

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.pages.PageTransformer

object JavadocTransformer : PageTransformer {
    override fun invoke(input: RootPageNode) =
        RootIndexPage("", input.children.map { visit(it) }, input)

    fun visit(page: PageNode, path: String = ""): PageNode = when (page) {
        is PackagePageNode -> visitPackagePageNode(page, path)
        is ClasslikePageNode -> visitClasslikePageNode(page, path)
        else -> page
    }

    private fun visitPackagePageNode(page: PackagePageNode, path: String): JavadocPageNode {
        val pathToRoot = getPathToRoot(path)
        return JavadocPageNode(page.name) {
            this.path = "$path/${page.name}".removePrefix("/")
            filename = "package-summary"

            children = page.children.map { visit(it, this.path) }
            contents {
                pageStart {
                    title = page.name
                    version = "0.0.1" // todo extract version
                    documentTitle = page.name
                    this.pathToRoot = pathToRoot
                }
                topNavbar {
                    this.page = this@JavadocPageNode
                    this.pathToRoot = pathToRoot
                }
                indexPage {
                    title = page.name
                    version = "0.0.1"
                    elems = this@JavadocPageNode.children
                    tabTitle = "Class Summary"
                    colTitle = "Class"
                }
            }
        }
    }

    private fun visitClasslikePageNode(page: ClasslikePageNode, path: String): JavadocPageNode {
        val pathToRoot = getPathToRoot(path)
        return JavadocPageNode(page.name) {
            this.path = path
            filename = page.name
            pageType = PageType.Class

            children = children.map { visit(it, this.path) }
            contents {
                pageStart {
                    title = page.name
                    version = "0.0.1" // todo extract version
                    documentTitle = page.name
                    this.pathToRoot = pathToRoot
                }
                topNavbar {
                    this.page = this@JavadocPageNode
                    this.pathToRoot = pathToRoot
                }
                // todo stub
                indexPage {
                    title = page.name
                    version = "0.0.1"
                    elems = this@JavadocPageNode.children
                    tabTitle = "Class stub"
                    colTitle = "Class insides"
                }
            }
        }
    }

    private fun getPathToRoot(path: String) = path.split("/").joinToString("/") { ".." }.let {
        if (it.isNotEmpty()) "$it/" else it
    }
}

object ResourcesInstaller : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.modified(
        children = input.children +
                RendererSpecificResourcePage(
                    "resourcePack",
                    emptyList(),
                    RenderingStrategy.Copy("static_res")
                )
    )
}