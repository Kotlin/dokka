package javadoc.pages

import kotlinx.html.html
import org.jetbrains.dokka.javadoc.JavadocRenderer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.renderers.html.HtmlRenderer
import org.jetbrains.dokka.renderers.html.NavigationNode
import org.jetbrains.dokka.renderers.html.NavigationPage
import org.jetbrains.dokka.renderers.platforms
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer
import java.time.LocalDate
import kotlinx.html.stream.createHTML

internal const val jQueryVersion = "3.3.1"
internal const val jQueryMigrateVersion = "3.0.1"

object RootCreator : PageNodeTransformer {
    override fun invoke(input: RootPageNode) =
        RendererSpecificRootPage("", listOf(input), RenderingStrategy.DoNothing)
}

object PackageSummaryInstaller : PageNodeTransformer {
    override fun invoke(input: RootPageNode) = input.modified(
        children = input.children.filterNot { it is ContentPage } +
                input.children.filterIsInstance<ContentPage>().single().children.map(::visit)
    )

    private fun visit(page: PageNode): PageNode = if (page is PackagePageNode) {
        page.modified(children = page.children + PackageSummary(page))
    } else {
        page
    }
}

class PackageSummary(val page: PackagePageNode) : RendererSpecificPage {
    override val name = "package-summary"
    override val children = emptyList<PageNode>()
    override fun modified(name: String, children: List<PageNode>) = this

    override val strategy = RenderingStrategy.Write(content())

    private fun content(): String = pageStart(page.name, "0.0.1", page.name, "???") + // TODO
            topNavbar(page, "???")

}

internal fun pageStart(title: String, version: String, documentTitle: String, pathToRoot: String) = """<!DOCTYPE HTML>
<!-- NewPage -->
<html lang="en">
<head>
<title>$documentTitle ($title $version API)</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta name="dc.created" content="${LocalDate.now()}">
<link rel="stylesheet" type="text/css" href="${pathToRoot}stylesheet.css" title="Style">
<link rel="stylesheet" type="text/css" href="${pathToRoot}jquery/jquery-ui.css" title="Style">
<script type="text/javascript" src="${pathToRoot}script.js"></script>
<script type="text/javascript" src="${pathToRoot}jquery/jszip/dist/jszip.min.js"></script>
<script type="text/javascript" src="${pathToRoot}jquery/jszip-utils/dist/jszip-utils.min.js"></script>
<!--[if IE]>
<script type="text/javascript" src="${pathToRoot}jquery/jszip-utils/dist/jszip-utils-ie.min.js"></script>
<![endif]-->
<script type="text/javascript" src="${pathToRoot}jquery/jquery-$jQueryVersion.js"></script>
<script type="text/javascript" src="${pathToRoot}jquery/jquery-migrate-$jQueryMigrateVersion.js"></script>
<script type="text/javascript" src="${pathToRoot}jquery/jquery-ui.js"></script>
</head>
<body>
<script type="text/javascript"><!--
    try {
        if (location.href.indexOf('is-external=true') == -1) {
            parent.document.title="$documentTitle ($title $version API)";
        }
    }
    catch(err) {
    }
//-->
var pathtoroot = "$pathToRoot";
var useModuleDirectories = true;
loadScripts(document, 'script');</script>
<noscript>
<div>JavaScript is disabled on your browser.</div>
</noscript>
<header role="banner">
<nav role="navigation">
<div class="fixedNav">""".trimIndent()

internal fun topNavbar(page: ContentPage, pathToRoot: String): String {
    val start = """<!-- ========= START OF TOP NAVBAR ======= -->
<div class="topNav"><a id="navbar.top">
<!--   -->
</a>
<div class="skipNav"><a href="#skip.navbar.top" title="Skip navigation links">Skip navigation links</a></div>
<a id="navbar.top.firstrow">
<!--   -->
</a>
<ul class="navList" title="Navigation">
""".trimIndent()

    val overviewLi = if (page is ClasslikePageNode)
        "<li><a href=\"${pathToRoot}index.html\">Overview</a></li>"
    else
        "<li><a href=\"package-summary.html\">Package</a></li>\n"

    val packageLi = if (page is ClasslikePageNode)
        "<li class=\"navBarCell1Rev\">Package</li>"
    else
        "<li>Package</li>"


    """<li class="navBarCell1Rev">Overview</li>
<li class="navBarCell1Rev">Package</li>
<li navBarCell1Rev>Class</li>
<li><a href="overview-tree.html">Tree</a></li>
<li><a href="${pathToRoot}deprecated-list.html">Deprecated</a></li>
<li><a href="${pathToRoot}index-all.html">Index</a></li>
<li><a href="${pathToRoot}help-doc.html">Help</a></li>
""".trimIndent()

    val end = """</ul>
</div>
<div class="subNav">
<ul class="navListSearch">
<li><label for="search">SEARCH:</label>
<input type="text" id="search" value="search" disabled="disabled">
<input type="reset" id="reset" value="reset" disabled="disabled">
</li>
</ul>
</div>
<a id="skip.navbar.top">
<!--   -->
</a>
<!-- ========= END OF TOP NAVBAR ========= -->"""
    return start
}

internal fun rootIndexPage(title: String, version: String) = """

</div>
<div class="navPadding">&nbsp;</div>
<script type="text/javascript"><!--
${'$'}('.navPadding').css('padding-top', ${'$'}('.fixedNav').css("height"));
//-->
</script>
</nav>
</header>
<main role="main">
<div class="header">
<h1 class="title">$title $version API</h1>
</div>
<div class="contentContainer">
<div class="overviewSummary">
<table>
<caption><span>Packages</span><span class="tabEnd">&nbsp;</span></caption>
<tr>
<th class="colFirst" scope="col">Package</th>
<th class="colLast" scope="col">Description</th>
</tr>
<tbody>
<tr class="altColor" id="i0">
<th class="colFirst" scope="row"><a href="adaptation/package-summary.html">adaptation</a></th>
<td class="colLast">&nbsp;</td>
</tr>
<tr class="rowColor" id="i1">
<th class="colFirst" scope="row"><a href="app/package-summary.html">app</a></th>
<td class="colLast">&nbsp;</td>
</tr>
<tr class="altColor" id="i2">
<th class="colFirst" scope="row"><a href="common/package-summary.html">common</a></th>
<td class="colLast">&nbsp;</td>
</tr>
<tr class="rowColor" id="i3">
<th class="colFirst" scope="row"><a href="model/package-summary.html">model</a></th>
<td class="colLast">&nbsp;</td>
</tr>
<tr class="altColor" id="i4">
<th class="colFirst" scope="row"><a href="processor/package-summary.html">processor</a></th>
<td class="colLast">&nbsp;</td>
</tr>
<tr class="rowColor" id="i5">
<th class="colFirst" scope="row"><a href="transformation/package-summary.html">transformation</a></th>
<td class="colLast">&nbsp;</td>
</tr>
</tbody>
</table>
</div>
</div>
</main>
<footer role="contentinfo">
<nav role="navigation">
<!-- ======= START OF BOTTOM NAVBAR ====== -->
<div class="bottomNav"><a id="navbar.bottom">
<!--   -->
</a>
<div class="skipNav"><a href="#skip.navbar.bottom" title="Skip navigation links">Skip navigation links</a></div>
<a id="navbar.bottom.firstrow">
<!--   -->
</a>
<ul class="navList" title="Navigation">
<li class="navBarCell1Rev">Overview</li>
<li>Package</li>
<li>Class</li>
<li><a href="overview-tree.html">Tree</a></li>
<li><a href="deprecated-list.html">Deprecated</a></li>
<li><a href="index-all.html">Index</a></li>
<li><a href="help-doc.html">Help</a></li>
</ul>
</div>
<a id="skip.navbar.bottom">
<!--   -->
</a>
<!-- ======== END OF BOTTOM NAVBAR ======= -->
</nav>
</footer>
</body>
</html>
""".trimIndent()

internal fun String.wrapInTag(tag: String, options: Map<String, String>) =
    "<$tag ${options.map { it.key + "=\"" + it.value + '"' }}>$this</$tag>"

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

//    private fun navLista(): String {
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
        """<nav role="navigation">
<!-- ======= START OF BOTTOM NAVBAR ====== -->
<div class="bottomNav"><a id="navbar.bottom">
<!--   -->
</a>
<div class="skipNav"><a href="#skip.navbar.bottom" title="Skip navigation links">Skip navigation links</a></div>
<a id="navbar.bottom.firstrow">
<!--   -->
</a>
${navList()}
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