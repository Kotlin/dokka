package renderers.gfm

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.TestPage
import renderers.defaultSourceSet
import java.io.File

class SourceSetDependentHintTest : GfmRenderingOnlyTestBase() {

    private val pl1 = defaultSourceSet.copy(
        "root",
        "pl1",
        DokkaSourceSetID("root", "pl1"),
        analysisPlatform = Platform.js,
        sourceRoots = setOf(File("pl1"))
    )
    private val pl2 = defaultSourceSet.copy(
        "root",
        "pl2",
        DokkaSourceSetID("root", "pl2"),
        analysisPlatform = Platform.jvm,
        sourceRoots = setOf(File("pl1"))
    )
    private val pl3 = defaultSourceSet.copy(
        "root",
        "pl3",
        DokkaSourceSetID("root", "pl3"),
        analysisPlatform = Platform.native,
        sourceRoots = setOf(File("pl1"))
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
        assert(renderedContent == "//[testPage](test-page.md)\n\n [pl1, pl2, pl3] abc  \n   \n")
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
        assert(renderedContent == "//[testPage](test-page.md)\n\n [pl1] a  \n   \n [pl2] b  \n   \n [pl3] c  \n   \n")
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
        assert(renderedContent == "//[testPage](test-page.md)\n\n [pl1] ab  \n   \n [pl2] bc  \n   \n")
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
        assert(renderedContent == "//[testPage](test-page.md)\n\n [pl1, pl2] ab  \n   \n")
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
        assert(renderedContent == "//[testPage](test-page.md)\n\n [pl1] ab  \n  \n   \n [pl2] a  \nb  \n   \n")
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
        assert(renderedContent == "//[testPage](test-page.md)\n\n [pl1, pl2] ab   \n")
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
        assert(renderedContent == "//[testPage](test-page.md)\n\n [pl1, pl2] a  \n   \n [pl3] b  \n   \n")
    }
}
