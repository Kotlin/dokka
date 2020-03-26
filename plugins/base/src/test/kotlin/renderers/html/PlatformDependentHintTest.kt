package renderers.html

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.Div
import renderers.RenderingOnlyTestBase
import renderers.TestPage
import renderers.match

class PlatformDependentHintTest : RenderingOnlyTestBase() {
    private val pl1 = PlatformData("pl1", Platform.js, listOf("pl1"))
    private val pl2 = PlatformData("pl2", Platform.jvm, listOf("pl2"))
    private val pl3 = PlatformData("pl3", Platform.native, listOf("pl3"))

    @Test
    fun platformIndependentCase() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2, pl3), styles = setOf(TextStyle.Block)) {
                text("a")
                text("b")
                text("c")
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div("abc"))
    }

    @Test
    fun completelyDivergentCase() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2, pl3), styles = setOf(TextStyle.Block)) {
                text("a", platformData = setOf(pl1))
                text("b", platformData = setOf(pl2))
                text("c", platformData = setOf(pl3))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match("[pl1]", Div("a"), "[pl2]",  Div("b"), "[pl3]", Div("c"))
    }

    @Test
    fun overlappingCase() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2), styles = setOf(TextStyle.Block)) {
                text("a", platformData = setOf(pl1))
                text("b", platformData = setOf(pl1, pl2))
                text("c", platformData = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match("[pl1]", Div("ab"), "[pl2]", Div("bc"))
    }

    @Test
    fun caseThatCanBeSimplified() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2), styles = setOf(TextStyle.Block)) {
                text("a", platformData = setOf(pl1, pl2))
                text("b", platformData = setOf(pl1))
                text("b", platformData = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div("ab"))
    }

    @Test
    fun caseWithGroupBreakingSimplification() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2), styles = setOf(TextStyle.Block)) {
                group(styles = setOf(TextStyle.Block)) {
                    text("a", platformData = setOf(pl1, pl2))
                    text("b", platformData = setOf(pl1))
                }
                text("b", platformData = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match("[pl1]", Div(Div("ab")), "[pl2]", Div(Div("a"), "b"))
    }

    @Test
    fun caseWithGroupNotBreakingSimplification() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2)) {
                group {
                    text("a", platformData = setOf(pl1, pl2))
                    text("b", platformData = setOf(pl1))
                }
                text("b", platformData = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        println(renderedContent)
        renderedContent.match("ab")
    }

    @Test
    fun partiallyUnifiedCase() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2, pl3), styles = setOf(TextStyle.Block)) {
                text("a", platformData = setOf(pl1))
                text("a", platformData = setOf(pl2))
                text("b", platformData = setOf(pl3))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match("[pl1, pl2]", Div("a"), "[pl3]",  Div("b"))
    }
}