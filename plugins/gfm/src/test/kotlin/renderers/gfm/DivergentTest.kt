package renderers.gfm

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ContentDivergentGroup
import org.junit.jupiter.api.Test
import renderers.testPage
import testApi.testRunner.defaultSourceSet
import java.io.File

class DivergentTest : GfmRenderingOnlyTestBase() {
    private val js = defaultSourceSet.copy(
        "js",
        DokkaSourceSetID("root", "js"),
        analysisPlatform = Platform.js,
        sourceRoots = setOf(File("pl1"))
    )
    private val jvm = defaultSourceSet.copy(
        "jvm",
        DokkaSourceSetID("root", "jvm"),
        analysisPlatform = Platform.jvm,
        sourceRoots = setOf(File("pl1"))
    )
    private val native = defaultSourceSet.copy(
        "native",
        DokkaSourceSetID("root", "native"),
        analysisPlatform = Platform.native,
        sourceRoots = setOf(File("pl1"))
    )

    @Test
    fun simpleWrappingCase() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[js]\
                        |a""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun noPlatformHintCase() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test"), implicitlySourceSetHinted = false) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[js]\
                        |a""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentBetweenSourceSets() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(jvm)) {
                    divergent {
                        text("b")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("c")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[js, jvm, native]\
                        |[js]\
                        |a
                        |
                        |[jvm]\
                        |b
                        |
                        |[native]\
                        |c""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInOneSourceSet() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(js)) {
                    divergent {
                        text("b")
                    }
                }
                instance(setOf(DRI("test", "Test3")), setOf(js)) {
                    divergent {
                        text("c")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[js]\
                        |a
                        |
                        |b
                        |
                        |c""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInAndBetweenSourceSets() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("b")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(jvm)) {
                    divergent {
                        text("c")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(js)) {
                    divergent {
                        text("d")
                    }
                }
                instance(setOf(DRI("test", "Test3")), setOf(native)) {
                    divergent {
                        text("e")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[native, js, jvm]\
                        |[native]\
                        |a
                        |
                        |[js]\
                        |b
                        |
                        |[jvm]\
                        |c
                        |
                        |[js]\
                        |d
                        |
                        |[native]\
                        |e""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInAndBetweenSourceSetsWithGrouping() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("a")
                    }
                    after {
                        text("a+")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("b")
                    }
                    after {
                        text("bd+")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(jvm)) {
                    divergent {
                        text("c")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(js)) {
                    divergent {
                        text("d")
                    }
                    after {
                        text("bd+")
                    }
                }
                instance(setOf(DRI("test", "Test3")), setOf(native)) {
                    divergent {
                        text("e")
                    }
                    after {
                        text("e+")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[native]\
                        |a
                        |
                        |a+
                        |
                        |[js]\
                        |b
                        |
                        |d
                        |
                        |bd+
                        |
                        |[jvm]\
                        |c
                        |
                        |[native]\
                        |e
                        |
                        |e+""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentSameBefore() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("b")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[native]\
                        |ab-
                        |
                        |a
                        |
                        |b""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentSameAfter() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("a")
                    }
                    after {
                        text("ab+")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    divergent {
                        text("b")
                    }
                    after {
                        text("ab+")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[native]\
                        |a
                        |
                        |b
                        |
                        |ab+""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentGroupedByBeforeAndAfter() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("a")
                    }
                    after {
                        text("ab+")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("b")
                    }
                    after {
                        text("ab+")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[native]\
                        |ab-
                        |
                        |a
                        |
                        |b
                        |
                        |ab+""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentDifferentBeforeAndAfter() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    before {
                        text("a-")
                    }
                    divergent {
                        text("a")
                    }
                    after {
                        text("ab+")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    before {
                        text("b-")
                    }
                    divergent {
                        text("b")
                    }
                    after {
                        text("ab+")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[native]\
                        |a-
                        |
                        |a
                        |
                        |ab+
                        |
                        |[native]\
                        |b-
                        |
                        |b
                        |
                        |ab+""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInAndBetweenSourceSetsWithGroupingAncCommonParts() {
        val page = testPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("a")
                    }
                    after {
                        text("a+")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("b")
                    }
                    after {
                        text("bd+")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(jvm)) {
                    divergent {
                        text("c")
                    }
                    after {
                        text("bd+")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(js)) {
                    divergent {
                        text("d")
                    }
                    after {
                        text("bd+")
                    }
                }
                instance(setOf(DRI("test", "Test3")), setOf(native)) {
                    divergent {
                        text("e")
                    }
                    after {
                        text("e+")
                    }
                }
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |[native]\
                        |a
                        |
                        |a+
                        |
                        |[js, jvm]\
                        |[js]\
                        |b
                        |
                        |[jvm]\
                        |c
                        |
                        |[js]\
                        |d
                        |
                        |bd+
                        |
                        |[native]\
                        |e
                        |
                        |e+""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }
}
