package renderers.html

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.SourceRootImpl
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ContentDivergentGroup
import org.junit.jupiter.api.Test
import renderers.*

class DivergentTest : RenderingOnlyTestBase() {
    private val js = defaultSourceSet.copy(
        "root",
        "JS",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "js"),
        analysisPlatform = Platform.js,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )
    private val jvm = defaultSourceSet.copy(
        "root",
        "JVM",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "jvm"),

        analysisPlatform = Platform.jvm,
        sourceRoots = listOf(SourceRootImpl("pl1"))
    )
    private val native = defaultSourceSet.copy(
        "root",
        "NATIVE",
        defaultSourceSet.sourceSetID.copy(sourceSetName = "native"),
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
        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div(Div("a")))))
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
        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div("a")))
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

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div(Div("a"), Div("b"), Div("c")))))
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

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div((Div(Div("abc"))))))
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

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div(Div("ae"), Div("bd"), Div("c")))))
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

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(Div(Span(Div(Div("NATIVE")))), Div(Div(Div("a"))), "a+"),
            Div(Div(Span(Div(Div("JS")))), Div(Div(Div("bd"))), "bd+"),
            Div(Div(Span(Div(Div("JVM")))), Div(Div(Div("c")))),
            Div(Div(Span(Div(Div("NATIVE")))), Div(Div(Div("e"))), "e+"),
        )
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

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(
                Div(
                    "ab-",
                    Span()
                ),
                Div(Div(Div("ab")))
            )
        )
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

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(
                Div(Div(Div("ab"))),
                "ab+"
            )
        )
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

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(
                Div("ab-", Span()),
                Div(Div(Div("ab"))),
                "ab+"
            )
        )
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

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(Div("a-", Span()), Div(Div(Div("a"))), "ab+"),
            Div(Div("b-", Span()), Div(Div(Div(("b")))), "ab+")
        )
    }
}
