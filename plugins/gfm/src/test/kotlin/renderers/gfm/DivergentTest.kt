package renderers.gfm

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.SourceRootImpl
import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ContentDivergentGroup
import org.junit.jupiter.api.Test
import renderers.defaultSourceSet
import renderers.TestPage

class DivergentTest : GfmRenderingOnlyTestBase() {
    private val js = defaultSourceSet.copy(
        "root",
        "JS",
        "js",
        analysisPlatform = Platform.js,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )
    private val jvm = defaultSourceSet.copy(
        "root",
        "JVM",
        "jvm",

        analysisPlatform = Platform.jvm,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )
    private val native = defaultSourceSet.copy(
        "root",
        "NATIVE",
        "native",
        analysisPlatform = Platform.native,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )

    @Test
    fun simpleWrappingCase() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
            }
        }
        val expect = "//[testPage](test-page.md)\n\n#### [root/js]  \n##### Content  \na  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun noPlatformHintCase() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test"), implicitlySourceSetHinted = false) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
            }
        }
        val expect = "//[testPage](test-page.md)\n\n#### [root/js]  \n##### Content  \na  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentBetweenSourceSets() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/js, root/jvm, root/native]  \n##### Content  \n###### [root/js]  \na  \n###### [root/jvm]  \nb  \n###### [root/native]  \nc  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInOneSourceSet() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/js]  \n##### Content  \na  \nb  \nc  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInAndBetweenSourceSets() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/native, root/js, root/jvm]  \n##### Content  \n###### [root/native]  \na  \n###### [root/js]  \nb  \n###### [root/jvm]  \nc  \n###### [root/js]  \nd  \n###### [root/native]  \ne  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInAndBetweenSourceSetsWithGrouping() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/native]  \n##### Content  \na  \n##### More info  \na+  \n#### [root/js]  \n##### Content  \nb  \nd  \n##### More info  \nbd+  \n#### [root/jvm]  \n##### Content  \nc  \n#### [root/native]  \n##### Content  \ne  \n##### More info  \ne+  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentSameBefore() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/native]  \n##### Brief description  \nab-  \n##### Content  \na  \nb  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentSameAfter() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/native]  \n##### Content  \na  \nb  \n##### More info  \nab+  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentGroupedByBeforeAndAfter() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/native]  \n##### Brief description  \nab-  \n##### Content  \na  \nb  \n##### More info  \nab+  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentDifferentBeforeAndAfter() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/native]  \n##### Brief description  \na-  \n##### Content  \na  \n##### More info  \nab+  \n#### [root/native]  \n##### Brief description  \nb-  \n##### Content  \nb  \n##### More info  \nab+  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun divergentInAndBetweenSourceSetsWithGroupingAncCommonParts() {
        val page = TestPage {
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
        val expect = "//[testPage](test-page.md)\n\n#### [root/native]  \n##### Content  \na  \n##### More info  \na+  \n#### [root/js, root/jvm]  \n##### Content  \n###### [root/js]  \nb  \n###### [root/jvm]  \nc  \n###### [root/js]  \nd  \n##### More info  \nbd+  \n#### [root/native]  \n##### Content  \ne  \n##### More info  \ne+  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }
}