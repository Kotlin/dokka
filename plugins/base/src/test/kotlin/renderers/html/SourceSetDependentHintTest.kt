package renderers.html

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.testPage
import testApi.testRunner.defaultSourceSet
import utils.Div
import utils.match
import java.io.File

class SourceSetDependentHintTest : HtmlRenderingOnlyTestBase() {

    private val pl1 = defaultSourceSet.copy(
        "pl1",
        defaultSourceSet.sourceSetID.copy(sourceSetName =  "pl1"),
        analysisPlatform = Platform.js,
        sourceRoots = setOf(File("pl1"))
    )
    private val pl2 = defaultSourceSet.copy(
        "pl2",
        defaultSourceSet.sourceSetID.copy(sourceSetName =  "pl2"),
        analysisPlatform = Platform.jvm,
        sourceRoots = setOf(File("pl1"))
    )
    private val pl3 = defaultSourceSet.copy(
        "pl3",
        defaultSourceSet.sourceSetID.copy(sourceSetName =  "pl3"),
        analysisPlatform = Platform.native,
        sourceRoots = setOf(File("pl1"))
    )

    @Test
    fun platformIndependentCase() {
        val page = testPage {
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
        val page = testPage {
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
        val page = testPage {
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
        val page = testPage {
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
        val page = testPage {
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
        val page = testPage {
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
        val page = testPage {
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
