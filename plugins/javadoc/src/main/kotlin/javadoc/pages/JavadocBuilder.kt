package javadoc.pages

import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificPage
import org.jetbrains.dokka.pages.RenderingStrategy

enum class PageType {
    Package, Class, Function
}

@DslMarker
annotation class JavadocDsl

@JavadocDsl
abstract class PageComponent() {
    abstract fun build(): String
}

class PageStart() : PageComponent() {
    var title: String = ""
    var version: String = ""
    var documentTitle: String? = null
    var pathToRoot: String = ""

    override fun build() = pageStart(title, version, documentTitle ?: title, pathToRoot)
}

class TopNavbar() : PageComponent() {
    var page: PageNode? = null
    var pathToRoot: String = ""

    override fun build(): String = topNavbar(page!!, pathToRoot)
}

class IndexPage() : PageComponent() {
    var title: String = ""
    var version: String = ""
    var elems: List<PageNode> = emptyList()
    var tabTitle: String = "Packages"
    var colTitle: String = "Package"

    override fun build(): String = indexPage(title, version, tabTitle, colTitle, elems)
}

@JavadocDsl
class Contents : ArrayList<PageComponent>() {
    fun pageStart(block: PageStart.() -> Unit) = add(PageStart().also(block))
    fun topNavbar(block: TopNavbar.() -> Unit) = add(TopNavbar().also(block))
    fun indexPage(block: IndexPage.() -> Unit) = add(IndexPage().also(block))
}

@JavadocDsl
class JavadocPageNode(override val name: String) : RendererSpecificPage {
    val fullPath: String by lazy {if(path.isBlank()) filename else "$path/$filename"}

    var path: String = ""
    var filename: String = ""
    var components: MutableList<PageComponent> = mutableListOf()
    var pageType = PageType.Package
    override var children: List<PageNode> = emptyList()

    override val strategy: RenderingStrategy = RenderingStrategy.Callback {
        components.map { it.build() }.joinToString("\n")
    }

    fun contents(block: Contents.() -> Unit) {
        components.addAll(Contents().also(block))
    }
    override fun modified(name: String, children: List<PageNode>): PageNode = JavadocPageNode(name).also {
        it.components = components
        it.children = children
        it.path = path
        it.filename = filename
        it.pageType = pageType
    }

    companion object {
        operator fun invoke(name: String, block: JavadocPageNode.() -> Unit) = JavadocPageNode(name).also(block)
    }
}