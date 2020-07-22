package renderers.html

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultTabSortingStrategy
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.external.DokkaExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.DefaultLocationProviderFactory
import org.jetbrains.dokka.testApi.context.MockContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import renderers.RenderingOnlyTestBase
import utils.TestOutputWriter
import renderers.defaultSourceSet
import java.io.File

abstract class HtmlRenderingOnlyTestBase : RenderingOnlyTestBase<Element>() {

    protected val js = defaultSourceSet.copy(
        "root",
        "JS",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "js"),
        analysisPlatform = Platform.js,
        sourceRoots = setOf(File("pl1"))
    )
    protected val jvm = defaultSourceSet.copy(
        "root",
        "JVM",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "jvm"),

        analysisPlatform = Platform.jvm,
        sourceRoots = setOf(File("pl1"))
    )
    protected val native = defaultSourceSet.copy(
        "root",
        "NATIVE",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "native"),
        analysisPlatform = Platform.native,
        sourceRoots = setOf(File("pl1"))
    )

    val files = TestOutputWriter()
    override val context = MockContext(
        DokkaBase().outputWriter to { _ -> files },
        DokkaBase().locationProviderFactory to ::DefaultLocationProviderFactory,
        DokkaBase().htmlPreprocessors to { _ -> RootCreator },
        DokkaBase().externalLocationProviderFactory to { ::JavadocExternalLocationProviderFactory },
        DokkaBase().externalLocationProviderFactory to { ::DokkaExternalLocationProviderFactory },
        DokkaBase().tabSortingStrategy to { DefaultTabSortingStrategy() },
        testConfiguration = DokkaConfigurationImpl(
            sourceSets = listOf(js, jvm, native)
        )
    )

    override val renderedContent: Element by lazy {
        files.contents.getValue("test-page.html").let { Jsoup.parse(it) }.select("#content").single()
    }

    protected fun linesAfterContentTag() =
        files.contents.getValue("test-page.html").lines()
            .dropWhile { !it.contains("""<div id="content">""") }
            .joinToString(separator = "") { it.trim() }
}

fun Element.match(vararg matchers: Any): Unit =
    childNodes()
        .filter { it !is TextNode || it.text().isNotBlank() }
        .let { it.drop(it.size - matchers.size) }
        .zip(matchers)
        .forEach { (n, m) -> m.accepts(n) }

open class Tag(val name: String, vararg val matchers: Any)
class Div(vararg matchers: Any) : Tag("div", *matchers)
class P(vararg matchers: Any) : Tag("p", *matchers)
class Span(vararg matchers: Any) : Tag("span", *matchers)

private fun Any.accepts(n: Node) {
    when (this) {
        is String -> assert(n is TextNode && n.text().trim() == this.trim()) { "\"$this\" expected but found: $n" }
        is Tag -> {
            assert(n is Element && n.tagName() == name) { "Tag $name expected but found: $n" }
            if (n is Element && matchers.isNotEmpty()) n.match(*matchers)
        }
        else -> throw IllegalArgumentException("$this is not proper matcher")
    }
}
