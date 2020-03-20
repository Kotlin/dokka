package javadoc.pages

import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.time.LocalDate

internal const val jQueryVersion = "3.3.1"
internal const val jQueryMigrateVersion = "3.0.1"

object RootCreator : PageTransformer {
    override fun invoke(input: RootPageNode) =
        RootIndexPage("", listOf(input), input)
}

object PackageSummaryInstaller : PageTransformer {
    override fun invoke(input: RootPageNode) = input.modified(
        children = input.children.filterNot { it is ContentPage } +
                input.children.filterIsInstance<ContentPage>().single().children.map(::visit)
    )

    private fun visit(page: PageNode): PageNode = if (page is PackagePageNode) {
        page.modified(children = page.children + PackageSummary(page))
    } else {
        println("[Info] Package summary not appended to $page")
        page
    }
}

object ResourcesInstaller : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.modified(
        children = input.children +
                RendererSpecificResourcePage("resourcePack", emptyList(), RenderingStrategy.Copy("static_res"))
    )
}

class RootIndexPage(override val name: String, override val children: List<PageNode>, val root: RootPageNode) :
    RootPageNode(), RendererSpecificPage {
    override val strategy: RenderingStrategy = RenderingStrategy.Callback { content() }
    val version: String = "0.0.1"
    val pathToRoot: String = ""

    override fun modified(name: String, children: List<PageNode>): RootPageNode = RootIndexPage(name, children, root)

    private fun content() =
        pageStart(name, version, name, pathToRoot) +
                topNavbar(root, pathToRoot) +
                rootIndexPage(name, version, children.filterIsInstance<PackagePageNode>())

}

class PackageSummary(val page: PackagePageNode) : RendererSpecificPage {
    override val name = "package-summary"
    override val children = emptyList<PageNode>()
    override fun modified(name: String, children: List<PageNode>) = this

    override val strategy = RenderingStrategy.Write(content())

    private fun content(): String = pageStart(page.name, "0.0.1", page.name, "../") + // TODO
            topNavbar(page, "???")

}

internal fun pageStart(title: String, version: String, documentTitle: String, pathToRoot: String) = """
        |<!DOCTYPE HTML>
        |<!-- NewPage -->
        |<html lang="en">
        |<head>
        |<title>$documentTitle ($title $version API)</title>
        |<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        |<meta name="dc.created" content="${LocalDate.now()}">
        |<link rel="stylesheet" type="text/css" href="${pathToRoot}stylesheet.css" title="Style">
        |<link rel="stylesheet" type="text/css" href="${pathToRoot}jquery/jquery-ui.css" title="Style">
        |<script type="text/javascript" src="${pathToRoot}script.js"></script>
        |<script type="text/javascript" src="${pathToRoot}jquery/jszip/dist/jszip.min.js"></script>
        |<script type="text/javascript" src="${pathToRoot}jquery/jszip-utils/dist/jszip-utils.min.js"></script>
        |<!--[if IE]>
        |<script type="text/javascript" src="${pathToRoot}jquery/jszip-utils/dist/jszip-utils-ie.min.js"></script>
        |<![endif]-->
        |<script type="text/javascript" src="${pathToRoot}jquery/jquery-$jQueryVersion.js"></script>
        |<script type="text/javascript" src="${pathToRoot}jquery/jquery-migrate-$jQueryMigrateVersion.js"></script>
        |<script type="text/javascript" src="${pathToRoot}jquery/jquery-ui.js"></script>
        |</head>
        |<body>
        |<script type="text/javascript"><!--
        |    try {
        |        if (location.href.indexOf('is-external=true') == -1) {
        |            parent.document.title="$documentTitle ($title $version API)";
        |        }
        |    }
        |    catch(err) {
        |    }
        |//-->
        |var pathtoroot = "$pathToRoot";
        |var useModuleDirectories = true;
        |loadScripts(document, 'script');</script>
        |<noscript>
        |<div>JavaScript is disabled on your browser.</div>
        |</noscript>
        |<header role="banner">
        |<nav role="navigation">
        |<div class="fixedNav">""".trimMargin("|")

internal fun topNavbar(page: PageNode, pathToRoot: String): String = """
        |<!-- ========= START OF TOP NAVBAR ======= -->
        |<div class="topNav"><a id="navbar.top">
        |<!--   -->
        |</a>
        |<div class="skipNav"><a href="#skip.navbar.top" title="Skip navigation links">Skip navigation links</a></div>
        |<a id="navbar.top.firstrow">
        |<!--   -->
        |</a>
        |<ul class="navList" title="Navigation">
        ${if (page is ClasslikePageNode)
            "<li><a href=\"${pathToRoot}index.html\">Overview</a></li>"
        else if (page is ModulePageNode)
            "<li class=\"navBarCell1Rev\">Overview</li>"
        else
            "<li><a href=\"package-summary.html\">Package</a></li>\n"}
                        ${if (page is ClasslikePageNode)
            "<li class=\"navBarCell1Rev\">Package</li>"
        else
            "<li>Package</li>"}
        |<li navBarCell1Rev>Class</li>
        |<li><a href="overview-tree.html">Tree</a></li>
        |<li><a href="${pathToRoot}deprecated-list.html">Deprecated</a></li>
        |<li><a href="${pathToRoot}index-all.html">Index</a></li>
        |<li><a href="${pathToRoot}help-doc.html">Help</a></li>
        |</ul>
        |</div>
        |<div class="subNav">
        |<ul class="navListSearch">
        |<li><label for="search">SEARCH:</label>
        |<input type="text" id="search" value="search" disabled="disabled">
        |<input type="reset" id="reset" value="reset" disabled="disabled">
        |</li>
        |</ul>
        |</div>
        |<a id="skip.navbar.top">
        |<!--   -->
        |</a>
        |<!-- ========= END OF TOP NAVBAR ========= -->""".trimMargin("|")

internal fun rootIndexPage(title: String, version: String, packages: List<PackagePageNode>) = """
        |</div>
        |<div class="navPadding">&nbsp;</div>
        |<script type="text/javascript"><!--
        |${'$'}('.navPadding').css('padding-top', ${'$'}('.fixedNav').css("height"));
        |//-->
        |</script>
        |</nav>
        |</header>
        |<main role="main">
        |<div class="header">
        |<h1 class="title">$title $version API</h1>
        |</div>
        |<div class="contentContainer">
        |<div class="overviewSummary">
        |<table>
        |<caption><span>Packages</span><span class="tabEnd">&nbsp;</span></caption>
        |<tr>
        |<th class="colFirst" scope="col">Package</th>
        |<th class="colLast" scope="col">Description</th>
        |</tr>
        |<tbody>
        ${packages.mapIndexed { i, e -> e.generateLink(i) }.joinToString("\n")}
        |</tbody>
        |</table>
        |</div>
        |</div>
        |</main>
        |<footer role="contentinfo">
        |<nav role="navigation">
        |<!-- ======= START OF BOTTOM NAVBAR ====== -->
        |<div class="bottomNav"><a id="navbar.bottom">
        |<!--   -->
        |</a>
        |<div class="skipNav"><a href="#skip.navbar.bottom" title="Skip navigation links">Skip navigation links</a></div>
        |<a id="navbar.bottom.firstrow">
        |<!--   -->
        |</a>
        |<ul class="navList" title="Navigation">
        |<li class="navBarCell1Rev">Overview</li>
        |<li>Package</li>
        |<li>Class</li>
        |<li><a href="overview-tree.html">Tree</a></li>
        |<li><a href="deprecated-list.html">Deprecated</a></li>
        |<li><a href="index-all.html">Index</a></li>
        |<li><a href="help-doc.html">Help</a></li>
        |</ul>
        |</div>
        |<a id="skip.navbar.bottom">
        |<!--   -->
        |</a>
        |<!-- ======== END OF BOTTOM NAVBAR ======= -->
        |</nav>
        |</footer>
        |</body>
        |</html>
        |""".trimMargin("|")

internal fun String.wrapInTag(tag: String, options: Map<String, String>) =
    "<$tag ${options.map { it.key + "=\"" + it.value + '"' }.joinToString(" ")}>$this</$tag>"

fun PageNode.generateLink(i: Int) = "\n<tr class=\"altColor\" id=\"i$i\">\n" +
        name.wrapInTag("a", mapOf("href" to "$name/package-summary.html"))
    .wrapInTag("th", mapOf("class" to "colFirst", "scope" to "row")) + "<td class=\"colLast\">&nbsp;</td>\n</tr>"

internal enum class NavigableType {
    Overview, Package, Class, Tree, Deprecated, Index, Help
}

internal interface Navigable {
    val type: NavigableType
}

class NavbarGenerator(val page: PageNode) {
    val activeClass = "navBarCell1Rev"
    fun pathToRoot() = "???" // TODO
    val navItems = listOf("Overview", "Package", "Class", "Tree", "Deprecated", "Index", "Help")

    //    private fun items = navItems.map {itemLink}
//    fun navItemLink()
    val x = createHTML().html {}

    private fun navList(content: String) = """<ul class="navList" title="Navigation">
        $content
        </ul>
        """.trimIndent()

//    private fun navList(): String {
//        when (page) {
//            is PackagePageNode ->
//        }
//                """
//<li><a href="../index.html">Overview</a></li>
//<li><a href="package-summary.html">Package</a></li>
//<li class="navBarCell1Rev">Class</li>
//"""
//        val classItem = if (page is ClasslikePageNode) {
//            "Class".wrapInTag("li", mapOf("class" to activeClass))
//        }
//        val treeItem = if (page is ModulePageNode) {
//            "<li><a href=\"overview-tree.html\">Tree</a></li>\n"
//        } else {
//            "<li><a href=\"package-tree.html\">Tree</a></li>\n"
//        }
//
//        val navListEnd = """
//<li><a href="${pathToRoot()}deprecated-list.html">Deprecated</a></li>
//<li><a href="${pathToRoot()}index-all.html">Index</a></li>
//<li><a href="${pathToRoot()}help-doc.html">Help</a></li>
//""".trimIndent()
//    }

    private fun bottomNavbar(page: PageNode): String {
        return """<nav role="navigation">
<!-- ======= START OF BOTTOM NAVBAR ====== -->
<div class="bottomNav"><a id="navbar.bottom">
<!--   -->
</a>
<div class="skipNav"><a href="#skip.navbar.bottom" title="Skip navigation links">Skip navigation links</a></div>
<a id="navbar.bottom.firstrow">
<!--   -->
</a>
${navList("????")}
</div>
<div class="subNav">
<div>
<ul class="subNavList">
<li>Summary:&nbsp;</li>
<li>Nested&nbsp;|&nbsp;</li>
<li>Field&nbsp;|&nbsp;</li>
<li><a href="#constructor.summary">Constr</a>&nbsp;|&nbsp;</li>
<li><a href="#method.summary">Method</a></li>
</ul>
<ul class="subNavList">
<li>Detail:&nbsp;</li>
<li>Field&nbsp;|&nbsp;</li>
<li><a href="#constructor.detail">Constr</a>&nbsp;|&nbsp;</li>
<li><a href="#method.detail">Method</a></li>
</ul>
</div>
</div>
<a id="skip.navbar.bottom">
<!--   -->
</a>
<!-- ======== END OF BOTTOM NAVBAR ======= -->
</nav>"""
    }
}