package renderers.html

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.SourceRootImpl
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.Div
import renderers.RenderingOnlyTestBase
import renderers.TestPage
import renderers.match

class SourceSetDependentHintTest : RenderingOnlyTestBase() {
    private val pl1 = SourceSetData("root", "pl1", "pl3",Platform.js, listOf(SourceRootImpl("pl1")))
    private val pl2 = SourceSetData("root","pl2", "pl3", Platform.jvm, listOf(SourceRootImpl("pl1")))
    private val pl3 = SourceSetData("root","pl3", "pl3", Platform.native, listOf(SourceRootImpl("pl1")))

    @Test
    fun platformIndependentCase() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2, pl3), styles = setOf(TextStyle.Block)) {
                text("a")
                text("b")
                text("c")
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div("abc"))))
    }

    @Test
    fun completelyDivergentCase() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2, pl3), styles = setOf(TextStyle.Block)) {
                text("a", sourceSets = setOf(pl1))
                text("b", sourceSets = setOf(pl2))
                text("c", sourceSets = setOf(pl3))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div("a")), Div(Div("b")), Div(Div("c"))))
    }

    @Test
    fun overlappingCase() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2), styles = setOf(TextStyle.Block)) {
                text("a", sourceSets = setOf(pl1))
                text("b", sourceSets = setOf(pl1, pl2))
                text("c", sourceSets = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div("ab")), Div(Div("bc"))))
    }

    @Test
    fun caseThatCanBeSimplified() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2), styles = setOf(TextStyle.Block)) {
                text("a", sourceSets = setOf(pl1, pl2))
                text("b", sourceSets = setOf(pl1))
                text("b", sourceSets = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div("ab"))))
    }

    @Test
    fun caseWithGroupBreakingSimplification() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2), styles = setOf(TextStyle.Block)) {
                group(styles = setOf(TextStyle.Block)) {
                    text("a", sourceSets = setOf(pl1, pl2))
                    text("b", sourceSets = setOf(pl1))
                }
                text("b", sourceSets = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div(Div("ab"))), Div(Div(Div("a"), "b"))))
    }

    @Test
    fun caseWithGroupNotBreakingSimplification() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2)) {
                group {
                    text("a", sourceSets = setOf(pl1, pl2))
                    text("b", sourceSets = setOf(pl1))
                }
                text("b", sourceSets = setOf(pl2))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div("ab")))
    }

    @Test
    fun partiallyUnifiedCase() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2, pl3), styles = setOf(TextStyle.Block)) {
                text("a", sourceSets = setOf(pl1))
                text("a", sourceSets = setOf(pl2))
                text("b", sourceSets = setOf(pl3))
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div("a")), Div(Div("b"))))
    }
}