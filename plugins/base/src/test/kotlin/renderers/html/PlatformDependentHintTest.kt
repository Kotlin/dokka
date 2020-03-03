package renderers.html

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.pages.Style
import org.jetbrains.dokka.pages.TextStyle
import org.junit.Test
import renderers.RenderingOnlyTestBase
import renderers.TestPage

class PlatformDependentHintTest: RenderingOnlyTestBase() {
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
        assert(linesAfterContentTag().contains("<div>abc</div></div>"))
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
        assert(linesAfterContentTag().contains("<div>a</div> [pl1]<div>b</div> [pl2]<div>c</div> [pl3]</div>"))
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
        assert(linesAfterContentTag().contains("<div>ab</div> [pl1]<div>bc</div> [pl2]</div>"))
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
        assert(linesAfterContentTag().contains("<div>ab</div></div>"))
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
        assert(linesAfterContentTag().contains("<div><div>ab</div></div> [pl1]<div><div>a</div>b</div> [pl2]</div>"))
    }

    @Test
    fun caseWithGroupNotBreakingSimplification() {
        val page = TestPage {
            platformDependentHint(platformData = setOf(pl1, pl2), styles = setOf(TextStyle.Block)) {
                group {
                    text("a", platformData = setOf(pl1, pl2))
                    text("b", platformData = setOf(pl1))
                }
                text("b", platformData = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        assert(linesAfterContentTag().contains("<div>ab</div></div>"))
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
        assert(linesAfterContentTag().contains("<div>a</div> [pl1, pl2]<div>b</div> [pl3]</div>"))
    }
}