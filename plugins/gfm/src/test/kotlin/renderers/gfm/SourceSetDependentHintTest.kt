package renderers.gfm

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.testPage
import testApi.testRunner.defaultSourceSet
import java.io.File

class SourceSetDependentHintTest : GfmRenderingOnlyTestBase() {

    private val pl1 = defaultSourceSet.copy(
        "pl1",
        DokkaSourceSetID("root", "pl1"),
        analysisPlatform = Platform.js,
        sourceRoots = setOf(File("pl1"))
    )
    private val pl2 = defaultSourceSet.copy(
        "pl2",
        DokkaSourceSetID("root", "pl2"),
        analysisPlatform = Platform.jvm,
        sourceRoots = setOf(File("pl1"))
    )
    private val pl3 = defaultSourceSet.copy(
        "pl3",
        DokkaSourceSetID("root", "pl3"),
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
        val expect = """|//[testPage](test-page.md)
                        |
                        |[pl1, pl2, pl3]\
                        |abc""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
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
        val expect = """|//[testPage](test-page.md)
                        |
                        |[pl1]\
                        |a
                        |
                        |[pl2]\
                        |b
                        |
                        |[pl3]\
                        |c""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
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
        val expect = """|//[testPage](test-page.md)
                        |
                        |[pl1]\
                        |ab
                        |
                        |[pl2]\
                        |bc""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
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
        val expect = """|//[testPage](test-page.md)
                        |
                        |[pl1, pl2]\
                        |ab""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
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
        val expect = """|//[testPage](test-page.md)
                        |
                        |[pl1]\
                        |ab
                        |
                        |[pl2]\
                        |a
                        |
                        |b""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
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
        val expect = """|//[testPage](test-page.md)
                        |
                        |[pl1, pl2]\
                        |ab""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
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
        val expect = """|//[testPage](test-page.md)
                        |
                        |[pl1, pl2]\
                        |a
                        |
                        |[pl3]\
                        |b""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }
}
