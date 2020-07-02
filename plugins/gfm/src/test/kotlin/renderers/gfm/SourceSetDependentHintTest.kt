package renderers.gfm

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.SourceRootImpl
import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.TestPage
import renderers.defaultSourceSet

class SourceSetDependentHintTest : GfmRenderingOnlyTestBase() {

    private val pl1 = defaultSourceSet.copy(
        "root",
        "pl1",
        "pl1",
        analysisPlatform = Platform.js,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )
    private val pl2 = defaultSourceSet.copy(
        "root",
        "pl2",
        "pl2",
        analysisPlatform = Platform.jvm,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )
    private val pl3 = defaultSourceSet.copy(
        "root",
        "pl3",
        "pl3",
        analysisPlatform = Platform.native,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )

    @Test
    fun platformIndependentCase() {
        val page = TestPage {
            sourceSetDependentHint(sourceSets = setOf(pl1, pl2, pl3), styles = setOf(TextStyle.Block)) {
                text("a")
                text("b")
                text("c")
            }
        }

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == "//[testPage](test-page.md)\n\n [root/pl1, root/pl2, root/pl3] abc  \n ")
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

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == "//[testPage](test-page.md)\n\n [root/pl1] a  \n  [root/pl2] b  \n  [root/pl3] c  \n ")
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

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == "//[testPage](test-page.md)\n\n [root/pl1] ab  \n  [root/pl2] bc  \n ")
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

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == "//[testPage](test-page.md)\n\n [root/pl1, root/pl2] ab  \n ")
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

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == "//[testPage](test-page.md)\n\n [root/pl1] ab  \n  \n  [root/pl2] a  \nb  \n ")
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

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == "//[testPage](test-page.md)\n\n [root/pl1, root/pl2] ab ")
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

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == "//[testPage](test-page.md)\n\n [root/pl1, root/pl2] a  \n  [root/pl3] b  \n ")
    }
}