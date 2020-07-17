package transformers

import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import matchers.content.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast

class CommentsToContentConverterTest {
    private val converter = DocTagToContentConverter

    private fun executeTest(
        docTag:DocTag,
        match: ContentMatcherBuilder<ContentComposite>.() -> Unit
    ) {
        val dci = DCI(
            setOf(
                DRI("kotlin", "Any")
            ),
            ContentKind.Comment
        )
        converter.buildContent(
            Li(
                listOf(
                    docTag
                )
            ),
            dci,
            emptySet()
        ).single().assertNode(match)
    }

    @Test
    fun `simple text`() {
        val docTag = P(listOf(Text("This is simple test of string Next line")))
        executeTest(docTag) {
            group { +"This is simple test of string Next line" }
        }
    }

    @Test
    fun `simple text with new line`() {
        val docTag = P(
            listOf(
                Text("This is simple test of string"),
                Br,
                Text("Next line")
            )
        )
        executeTest(docTag) {
            group {
                +"This is simple test of string"
                node<ContentBreakLine>()
                +"Next line"
            }
        }
    }

    @Test
    fun `paragraphs`() {
        val docTag = P(
            listOf(
                P(listOf(Text("Paragraph number one"))),
                P(listOf(Text("Paragraph"), Br, Text("number two")))
            )
        )
        executeTest(docTag) {
            group {
                group { +"Paragraph number one" }
                group {
                    +"Paragraph"
                    node<ContentBreakLine>()
                    +"number two"
                }
            }
        }
    }

    @Test
    fun `unordered list with empty lines`() {
        val docTag = Ul(
            listOf(
                Li(listOf(P(listOf(Text("list item 1 continue 1"))))),
                Li(listOf(P(listOf(Text("list item 2"), Br, Text("continue 2")))))
            )
        )
        executeTest(docTag) {
            node<ContentList> {
                group {
                    +"list item 1 continue 1"
                }
                group {
                    +"list item 2"
                    node<ContentBreakLine>()
                    +"continue 2"
                }
            }
        }
    }

    @Test
    fun `nested list`() {
        val docTag = P(
            listOf(
                Ul(
                    listOf(
                        Li(listOf(P(listOf(Text("Outer first Outer next line"))))),
                        Li(listOf(P(listOf(Text("Outer second"))))),
                        Ul(
                            listOf(
                                Li(listOf(P(listOf(Text("Middle first Middle next line"))))),
                                Li(listOf(P(listOf(Text("Middle second"))))),
                                Ul(
                                    listOf(
                                        Li(listOf(P(listOf(Text("Inner first Inner next line")))))
                                    )
                                ),
                                Li(listOf(P(listOf(Text("Middle third")))))
                            )
                        ),
                        Li(listOf(P(listOf(Text("Outer third")))))
                    )
                ),
                P(listOf(Text("New paragraph")))
            )
        )
        executeTest(docTag) {
            group {
                node<ContentList> {
                    group { +"Outer first Outer next line" }
                    group { +"Outer second" }
                    node<ContentList> {
                        group { +"Middle first Middle next line" }
                        group { +"Middle second" }
                        node<ContentList> {
                            group { +"Inner first Inner next line" }
                        }
                        group { +"Middle third" }
                    }
                    group { +"Outer third" }
                }
                group { +"New paragraph" }
            }
        }
    }

    @Test
    fun `header and paragraphs`() {
        val docTag = P(
            listOf(
                H1(listOf(Text("Header 1"))),
                P(listOf(Text("Following text"))),
                P(listOf(Text("New paragraph")))
            )
        )
        executeTest(docTag) {
            group {
                header(1) { +"Header 1" }
                group { +"Following text" }
                group { +"New paragraph" }
            }
        }
    }

    @Test
    fun `header levels`() {
        val docTag = P(
            listOf(
                H1(listOf(Text("Header 1"))),
                P(listOf(Text("Text 1"))),
                H2(listOf(Text("Header 2"))),
                P(listOf(Text("Text 2"))),
                H3(listOf(Text("Header 3"))),
                P(listOf(Text("Text 3"))),
                H4(listOf(Text("Header 4"))),
                P(listOf(Text("Text 4"))),
                H5(listOf(Text("Header 5"))),
                P(listOf(Text("Text 5"))),
                H6(listOf(Text("Header 6"))),
                P(listOf(Text("Text 6")))
            )
        )
        executeTest(docTag) {
            group {
                header(1) { +"Header 1" }
                group { +"Text 1" }
                header(2) { +"Header 2" }
                group { +"Text 2" }
                header(3) { +"Header 3" }
                group { +"Text 3" }
                header(4) { +"Header 4" }
                group { +"Text 4" }
                header(5) { +"Header 5" }
                group { +"Text 5" }
                header(6) { +"Header 6" }
                group { +"Text 6" }
            }
        }
    }

    @Test
    fun `block quotes`() {
        val docTag = P(
            listOf(
                BlockQuote(
                    listOf(
                        P(
                            listOf(
                                Text("Blockquotes are very handy in email to emulate reply text. This line is part of the same quote.")
                            )
                        )
                    )
                ),
                P(listOf(Text("Quote break."))),
                BlockQuote(
                    listOf(
                        P(listOf(Text("Quote")))
                    )
                )
            )
        )
        executeTest(docTag) {
            group {
                node<ContentCodeBlock> {
                    +"Blockquotes are very handy in email to emulate reply text. This line is part of the same quote."
                }
                group { +"Quote break." }
                node<ContentCodeBlock> {
                    +"Quote"
                }
            }
        }
    }

    @Test
    fun `nested block quotes`() {
        val docTag = P(
            listOf(
                BlockQuote(
                    listOf(
                        P(listOf(Text("text 1 text 2"))),
                        BlockQuote(
                            listOf(
                                P(listOf(Text("text 3 text 4")))
                            )
                        ),
                        P(listOf(Text("text 5")))
                    )
                ),
                P(listOf(Text("Quote break."))),
                BlockQuote(
                    listOf(
                        P(listOf(Text("Quote")))
                    )
                )
            )
        )
        executeTest(docTag) {
            group {
                node<ContentCodeBlock> {
                    +"text 1 text 2"
                    node<ContentCodeBlock> {
                        +"text 3 text 4"
                    }
                    +"text 5"
                }
                group { +"Quote break." }
                node<ContentCodeBlock> {
                    +"Quote"
                }
            }
        }
    }

    @Test
    fun `multiline code`() {
        val docTag = P(
            listOf(
                CodeBlock(
                    listOf(
                        Text("val x: Int = 0"), Br,
                        Text("val y: String = \"Text\""), Br, Br,
                        Text("    val z: Boolean = true"), Br,
                        Text("for(i in 0..10) {"), Br,
                        Text("    println(i)"), Br,
                        Text("}")
                    ),
                    mapOf("lang" to "kotlin")
                ),
                P(listOf(Text("Sample text")))
            )
        )
        executeTest(docTag) {
            group {
                node<ContentCodeBlock> {
                    +"val x: Int = 0"
                    node<ContentBreakLine>()
                    +"val y: String = \"Text\""
                    node<ContentBreakLine>()
                    node<ContentBreakLine>()
                    +"    val z: Boolean = true"
                    node<ContentBreakLine>()
                    +"for(i in 0..10) {"
                    node<ContentBreakLine>()
                    +"    println(i)"
                    node<ContentBreakLine>()
                    +"}"
                }
                group { +"Sample text" }
            }
        }
    }

    @Test
    fun `inline link`() {
        val docTag = P(
            listOf(
                A(
                    listOf(Text("I'm an inline-style link")),
                    mapOf("href" to "https://www.google.com")
                )
            )
        )
        executeTest(docTag) {
            group { link {
                +"I'm an inline-style link"
                check {
                    assertEquals(
                        assertedCast<ContentResolvedLink> { "Link should be resolved" }.address,
                        "https://www.google.com"
                    )
                }
            } }
        }
    }



    @Test
    fun `ordered list`() {
        val docTag =
            Ol(
            listOf(
                Li(
                    listOf(
                        P(listOf(Text("test1"))),
                        P(listOf(Text("test2"))),
                    )
                ),
                Li(
                    listOf(
                        P(listOf(Text("test3"))),
                        P(listOf(Text("test4"))),
                    )
                )
            )
        )
        executeTest(docTag) {
            node<ContentList> {
                group {
                        +"test1"
                        +"test2"
                }
                group {
                        +"test3"
                        +"test4"
                }
            }
        }
    }

    @Test
    fun `nested ordered list`() {
        val docTag = P(
            listOf(
                Ol(
                    listOf(
                        Li(listOf(P(listOf(Text("Outer first Outer next line"))))),
                        Li(listOf(P(listOf(Text("Outer second"))))),
                        Ol(
                            listOf(
                                Li(listOf(P(listOf(Text("Middle first Middle next line"))))),
                                Li(listOf(P(listOf(Text("Middle second"))))),
                                Ol(
                                    listOf(
                                        Li(listOf(P(listOf(Text("Inner first Inner next line")))))
                                    ),
                                    mapOf("start" to "1")
                                ),
                                Li(listOf(P(listOf(Text("Middle third")))))
                            ),
                            mapOf("start" to "1")
                        ),
                        Li(listOf(P(listOf(Text("Outer third")))))
                    ),
                    mapOf("start" to "1")
                ),
                P(listOf(Text("New paragraph")))
            )
        )
        executeTest(docTag) {
            group {
                node<ContentList> {
                    group { +"Outer first Outer next line" }
                    group { +"Outer second" }
                    node<ContentList> {
                        group { +"Middle first Middle next line" }
                        group { +"Middle second" }
                        node<ContentList> {
                            +"Inner first Inner next line"
                        }
                        group { +"Middle third" }
                    }
                    group { +"Outer third" }
                }
                group {
                    +"New paragraph"
                }
            }
        }
    }
}