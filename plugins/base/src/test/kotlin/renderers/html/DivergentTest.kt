/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ContentDivergentGroup
import renderers.testPage
import utils.Br
import utils.match
import kotlin.test.Test
import kotlin.test.assertEquals

class DivergentTest : HtmlRenderingOnlyTestBase() {

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
        HtmlRenderer(context).render(page)
        renderedContent.select("[data-togglable=DEFAULT/js]").single().match("a")
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
        HtmlRenderer(context).render(page)
        renderedContent.match("a")
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

        HtmlRenderer(context).render(page)
        val content = renderedContent
        content.select("[data-togglable=DEFAULT/js]").single().match("a")
        content.select("[data-togglable=DEFAULT/jvm]").single().match("b")
        content.select("[data-togglable=DEFAULT/native]").single().match("c")
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

        HtmlRenderer(context).render(page)
        renderedContent.select("[data-togglable=DEFAULT/js]").single().match("abc")
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

        HtmlRenderer(context).render(page)
        val content = renderedContent
        val orderOfTabs = content.select(".platform-bookmarks-row").single().children().map { it.attr("data-toggle") }

        assertEquals(listOf("DEFAULT/js", "DEFAULT/jvm", "DEFAULT/native"), orderOfTabs)

        content.select("[data-togglable=DEFAULT/native]").single().match("ae")
        content.select("[data-togglable=DEFAULT/js]").single().match("bd")
        content.select("[data-togglable=DEFAULT/jvm]").single().match("c")
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

        HtmlRenderer(context).render(page)
        val content = renderedContent
        content.select("[data-togglable=DEFAULT/native]").single().match("aa+", Br, "ee+")
        content.select("[data-togglable=DEFAULT/js]").single().match("bdbd+")
        content.select("[data-togglable=DEFAULT/jvm]").single().match("c")
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

        HtmlRenderer(context).render(page)
        renderedContent.select("[data-togglable=DEFAULT/native]").single().match("ab-ab")
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

        HtmlRenderer(context).render(page)
        renderedContent.select("[data-togglable=DEFAULT/native]").single().match("abab+")
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

        HtmlRenderer(context).render(page)
        renderedContent.select("[data-togglable=DEFAULT/native]").single().match("ab-abab+")
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

        HtmlRenderer(context).render(page)
        renderedContent.select("[data-togglable=DEFAULT/native]").single().match("a-aab+", Br, "b-bab+")
    }
}
