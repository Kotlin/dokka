package javadoc.pages

import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.PageNode
import java.time.LocalDate

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

    private fun bottomNavbar(page: PageNode): String =
        """
        |<nav role="navigation">
        |<!-- ======= START OF BOTTOM NAVBAR ====== -->
        |<div class="bottomNav"><a id="navbar.bottom">
        |<!--   -->
        |</a>
        |<div class="skipNav"><a href="#skip.navbar.bottom" title="Skip navigation links">Skip navigation links</a></div>
        |<a id="navbar.bottom.firstrow">
        |<!--   -->
        |</a>
        ${navList("????")}
        |</div>
        |<div class="subNav">
        |<div>
        |<ul class="subNavList">
        |<li>Summary:&nbsp;</li>
        |<li>Nested&nbsp;|&nbsp;</li>
        |<li>Field&nbsp;|&nbsp;</li>
        |<li><a href="#constructor.summary">Constr</a>&nbsp;|&nbsp;</li>
        |<li><a href="#method.summary">Method</a></li>
        |</ul>
        |<ul class="subNavList">
        |<li>Detail:&nbsp;</li>
        |<li>Field&nbsp;|&nbsp;</li>
        |<li><a href="#constructor.detail">Constr</a>&nbsp;|&nbsp;</li>
        |<li><a href="#method.detail">Method</a></li>
        |</ul>
        |</div>
        |</div>
        |<a id="skip.navbar.bottom">
        |<!--   -->
        |</a>
        |<!-- ======== END OF BOTTOM NAVBAR ======= -->
        |</nav>"""
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
        ${/*if ((page is JavadocPageNode) && page.pageType == PageType.Class)
            "<li><a href=\"${pathToRoot}index.html\">Overview</a></li>"
        else if (page is RootIndexPage)
            "<li class=\"navBarCell1Rev\">Overview</li>"
        else
            "<li><a href=\"package-summary.html\">Package</a></li>\n"}
        ${if ((page is JavadocPageNode) && page.pageType == PageType.Package)
            "<li class=\"navBarCell1Rev\">Package</li>"
        else
            "<li>Package</li>"*/ ""}
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

internal fun indexPage(title: String, version: String, tabTitle: String, colTitle: String, packages: List<PageNode>) = """
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
        |<caption><span>$tabTitle</span><span class="tabEnd">&nbsp;</span></caption>
        |<tr>
        |<th class="colFirst" scope="col">$colTitle</th>
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

fun classData(name: String, extends: String) = """
    |<!-- ======== START OF CLASS DATA ======== -->
    |<div class="header">
    |<div class="subTitle">adaptation</div>
    |<h2 title="Class $name" class="title">Class $name</h2>
    |</div>
    |<div class="contentContainer">
    ${classInheritance()}
    |<div class="description">
    |<ul class="blockList">
    |<li class="blockList">
    |<hr>
    |<br>
    |<pre>public class <span class="typeNameLabel">$name</span>
    |extends $name</pre>
    |</li>
    |</ul>
    |</div>
    |<div class="summary">
    |<ul class="blockList">
    |<li class="blockList">
    |<!-- ======== NESTED CLASS SUMMARY ======== -->
    |<ul class="blockList">
    |<li class="blockList"><a name="nested.class.summary">
    |<!--   -->
    |</a>
    |<h3>Nested Class Summary</h3>
    |<table class="memberSummary" border="0" cellpadding="3" cellspacing="0" summary="Nested Class Summary table, listing nested classes, and an explanation">
    |<caption><span>Nested Classes</span><span class="tabEnd">&nbsp;</span></caption>
    |<tr>
    |<th class="colFirst" scope="col">Modifier and Type</th>
    |<th class="colLast" scope="col">Class and Description</th>
    |</tr>
    |<tr class="altColor">
    |<td class="colFirst"><code>class&nbsp;</code></td>
    |<td class="colLast"><code><span class="memberNameLink"><a href="../adaptation/Adaptation.AdaptationInternalClass.html" title="class in adaptation">Adaptation.AdaptationInternalClass</a></span></code>&nbsp;</td>
    |</tr>
    |<tr class="rowColor">
    |<td class="colFirst"><code>static class&nbsp;</code></td>
    |<td class="colLast"><code><span class="memberNameLink"><a href="../adaptation/Adaptation.AdaptationInternalStaticClass.html" title="class in adaptation">Adaptation.AdaptationInternalStaticClass</a></span></code>&nbsp;</td>
    |</tr>
    |</table>
    |</li>
    |</ul>
    |<!-- ======== CONSTRUCTOR SUMMARY ======== -->
    |<ul class="blockList">
    |<li class="blockList"><a name="constructor.summary">
    |<!--   -->
    |</a>
    |<h3>Constructor Summary</h3>
    |<table class="memberSummary" border="0" cellpadding="3" cellspacing="0" summary="Constructor Summary table, listing constructors, and an explanation">
    |<caption><span>Constructors</span><span class="tabEnd">&nbsp;</span></caption>
    |<tr>
    |<th class="colOne" scope="col">Constructor and Description</th>
    |</tr>
    |<tr class="altColor">
    |<td class="colOne"><code><span class="memberNameLink"><a href="../adaptation/Adaptation.html#Adaptation--">Adaptation</a></span>()</code>&nbsp;</td>
    |</tr>
    |</table>
    |</li>
    |</ul>
    |<!-- ========== METHOD SUMMARY =========== -->
    |<ul class="blockList">
    |<li class="blockList"><a name="method.summary">
    |<!--   -->
    |</a>
    |<h3>Method Summary</h3>
    |<table class="memberSummary" border="0" cellpadding="3" cellspacing="0" summary="Method Summary table, listing methods, and an explanation">
    |<caption><span id="t0" class="activeTableTab"><span>All Methods</span><span class="tabEnd">&nbsp;</span></span><span id="t1" class="tableTab"><span><a href="javascript:show(1);">Static Methods</a></span><span class="tabEnd">&nbsp;</span></span><span id="t4" class="tableTab"><span><a href="javascript:show(8);">Concrete Methods</a></span><span class="tabEnd">&nbsp;</span></span></caption>
    |<tr>
    |<th class="colFirst" scope="col">Modifier and Type</th>
    |<th class="colLast" scope="col">Method and Description</th>
    |</tr>
    |<tr id="i0" class="altColor">
    |<td class="colFirst"><code>static org.javatuples.Pair&lt;<a href="../model/ModelGraph.html" title="class in model">ModelGraph</a>,java.lang.Boolean&gt;</code></td>
    |<td class="colLast"><code><span class="memberNameLink"><a href="../adaptation/Adaptation.html#transform-model.ModelGraph-transformation.Transformation-">transform</a></span>(<a href="../model/ModelGraph.html" title="class in model">ModelGraph</a>&nbsp;graph,
    |         <a href="../transformation/Transformation.html" title="interface in transformation">Transformation</a>&nbsp;transformation)</code>&nbsp;</td>
    |</tr>
    |</table>
    |<ul class="blockList">
    |<li class="blockList"><a name="methods.inherited.from.class.java.lang.Object">
    |<!--   -->
    |</a>
    |<h3>Methods inherited from class&nbsp;java.lang.Object</h3>
    |<code>clone, equals, finalize, getClass, hashCode, notify, notifyAll, toString, wait, wait, wait</code></li>
    |</ul>
    |</li>
    |</ul>
    |</li>
    |</ul>
    |</div>
    |<div class="details">
    |<ul class="blockList">
    |<li class="blockList">
    |<!-- ========= CONSTRUCTOR DETAIL ======== -->
    |<ul class="blockList">
    |<li class="blockList"><a name="constructor.detail">
    |<!--   -->
    |</a>
    |<h3>Constructor Detail</h3>
    |<a name="Adaptation--">
    |<!--   -->
    |</a>
    |<ul class="blockListLast">
    |<li class="blockList">
    |<h4>Adaptation</h4>
    |<pre>public&nbsp;Adaptation()</pre>
    |</li>
    |</ul>
    |</li>
    |</ul>
    |<!-- ============ METHOD DETAIL ========== -->
    |<ul class="blockList">
    |<li class="blockList"><a name="method.detail">
    |<!--   -->
    |</a>
    |<h3>Method Detail</h3>
    |<a name="transform-model.ModelGraph-transformation.Transformation-">
    |<!--   -->
    |</a>
    |<ul class="blockListLast">
    |<li class="blockList">
    |<h4>transform</h4>
    |<pre>public static&nbsp;org.javatuples.Pair&lt;<a href="../model/ModelGraph.html" title="class in model">ModelGraph</a>,java.lang.Boolean&gt;&nbsp;transform(<a href="../model/ModelGraph.html" title="class in model">ModelGraph</a>&nbsp;graph,
    |                                                                          <a href="../transformation/Transformation.html" title="interface in transformation">Transformation</a>&nbsp;transformation)</pre>
    |</li>
    |</ul>
    |</li>
    |</ul>
    |</li>
    |</ul>
    |</div>
    |</div>
    |<!-- ========= END OF CLASS DATA ========= -->
""".trimIndent()

fun classInheritance() = """
    |<ul class="inheritance">
    |<li>java.lang.Object</li>
    |<li>
    |<ul class="inheritance">
    |<li>adaptation.Adaptation</li>
    |</ul>
    |</li>
    |</ul>
    """

internal fun String.wrapInTag(tag: String, options: Map<String, String>) =
    "<$tag ${options.map { it.key + "=\"" + it.value + '"' }.joinToString(" ")}>$this</$tag>"

fun PageNode.generateLink(i: Int) = "\n<tr class=\"altColor\" id=\"i$i\">\n" + run {
    val path = /*if (this is JavadocPageNode && this.pageType != PageType.Package) "$filename.html" else*/ "$name/package-summary.html"

    name.wrapInTag("a", mapOf("href" to path))
        .wrapInTag(
            "th",
            mapOf("class" to "colFirst", "scope" to "row")
        ) + "<td class=\"colLast\">&nbsp;</td>\n</tr>"
}

internal enum class NavigableType {
    Overview, Package, Class, Tree, Deprecated, Index, Help
}

internal interface Navigable {
    val type: NavigableType
}