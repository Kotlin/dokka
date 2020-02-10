package org.jetbrains.dokka.tests

import markdown.KDocTest
import org.jetbrains.dokka.model.doc.*
import org.junit.Ignore
import org.junit.Test


class ParserTest : KDocTest() {

    @Test fun `Simple text`() {
        val kdoc = """
        | This is simple test of string
        | Next line
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(Text(body = "This is simple test of string\nNext line")))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Text with Bold and Emphasis decorators`() {
        val kdoc = """
        | This is **simple** test of _string_
        | Next **_line_**
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(
                        listOf(
                            Text(body = "This is "),
                            B(listOf(Text(body = "simple"))),
                            Text(body = " test of "),
                            I(listOf(Text(body = "string"))),
                            Text(body = "\nNext "),
                            B(listOf(I(listOf(Text(body = "line")))))
                        )
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Text with Colon`() {
        val kdoc = """
        | This is simple text with: colon!
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(Text(body = "This is simple text with: colon!")))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Multilined text`() {
        val kdoc = """
        | Text
        | and
        | String
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(Text(body = "Text\nand\nString")))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Paragraphs`() {
        val kdoc = """
        | Paragraph number
        | one
        |
        | Paragraph
        | number two
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(
                        listOf(
                            P(listOf(Text(body = "Paragraph number\none"))),
                            P(listOf(Text(body = "Paragraph\nnumber two")))
                        )
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Emphasis with star`() {
        val kdoc = "*text*"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(I(listOf(Text(body = "text")))))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Underscores that are not Emphasis`() {
        val kdoc = "text_with_underscores"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(Text(body = "text_with_underscores")))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Emphasis with underscores`() {
        val kdoc = "_text_"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(I(listOf(Text(body = "text")))))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Embedded star`() {
        val kdoc = "Embedded*Star"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(Text(body = "Embedded*Star")))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test fun `Unordered list`() {
        val kdoc = """
        | * list item 1
        | * list item 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    Ul(
                        listOf(
                            Li(listOf(P(listOf(Text(body = "list item 1"))))),
                            Li(listOf(P(listOf(Text(body = "list item 2")))))
                        )
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Unordered list with multilines`() {
        val kdoc = """
        | * list item 1
        |  continue 1
        | * list item 2
        | continue 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    Ul(
                        listOf(
                            Li(listOf(P(listOf(Text(body = "list item 1\ncontinue 1"))))),
                            Li(listOf(P(listOf(Text(body = "list item 2\ncontinue 2")))))
                        )
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Unordered list with Bold`() {
        val kdoc = """
        | * list **item** 1
        |  continue 1
        | * list __item__ 2
        |  continue 2
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    Ul(listOf(
                        Li(listOf(P(listOf(
                            Text(body = "list "),
                            B(listOf(Text(body = "item"))),
                            Text(body = " 1\ncontinue 1")
                        )))),
                        Li(listOf(P(listOf(
                            Text(body = "list "),
                            B(listOf(Text(body = "item"))),
                            Text(body = " 2\ncontinue 2")
                        ))))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Unordered list with nested bullets`() {
        val kdoc = """
        | * Outer first
        | Outer next line
        | * Outer second
        |     - Middle first
        | Middle next line
        |     - Middle second
        |         + Inner first
        | Inner next line
        |     - Middle third
        | * Outer third
        |
        | New paragraph""".trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        Ul(listOf(
                            Li(listOf(P(listOf(Text(body = "Outer first\nOuter next line"))))),
                            Li(listOf(P(listOf(Text(body = "Outer second"))))),
                            Ul(listOf(
                                Li(listOf(P(listOf(Text(body = "Middle first\nMiddle next line"))))),
                                Li(listOf(P(listOf(Text(body = "Middle second"))))),
                                Ul(listOf(
                                    Li(listOf(P(listOf(Text(body = "Inner first\nInner next line")))))
                                )),
                                Li(listOf(P(listOf(Text(body = "Middle third")))))
                            )),
                            Li(listOf(P(listOf(Text(body = "Outer third")))))
                        )),
                        P(listOf(Text(body = "New paragraph")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Ordered list`() {
        val kdoc = """
        | 1. list item 1
        | 2. list item 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    Ol(
                        listOf(
                            Li(listOf(P(listOf(Text(body = "list item 1"))))),
                            Li(listOf(P(listOf(Text(body = "list item 2")))))
                        ),
                        mapOf("start" to "1")
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test fun `Ordered list beginning from other number`() {
        val kdoc = """
        | 9. list item 1
        | 12. list item 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    Ol(
                        listOf(
                            Li(listOf(P(listOf(Text(body = "list item 1"))))),
                            Li(listOf(P(listOf(Text(body = "list item 2")))))
                        ),
                        mapOf("start" to "9")
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Ordered list with multilines`() {
        val kdoc = """
        | 2. list item 1
        |  continue 1
        | 3. list item 2
        | continue 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    Ol(
                        listOf(
                            Li(listOf(P(listOf(Text(body = "list item 1\ncontinue 1"))))),
                            Li(listOf(P(listOf(Text(body = "list item 2\ncontinue 2")))))
                        ),
                        mapOf("start" to "2")
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Ordered list with Bold`() {
        val kdoc = """
        | 1. list **item** 1
        |  continue 1
        | 2. list __item__ 2
        |  continue 2
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    Ol(listOf(
                            Li(listOf(P(listOf(
                                Text(body = "list "),
                                B(listOf(Text(body = "item"))),
                                Text(body = " 1\ncontinue 1")
                            )))),
                            Li(listOf(P(listOf(
                                Text(body = "list "),
                                B(listOf(Text(body = "item"))),
                                Text(body = " 2\ncontinue 2")
                            ))))
                        ),
                        mapOf("start" to "1")
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Ordered list with nested bullets`() {
        val kdoc = """
        | 1. Outer first
        | Outer next line
        | 2. Outer second
        |     1. Middle first
        | Middle next line
        |     2. Middle second
        |         1. Inner first
        | Inner next line
        |     5. Middle third
        | 4. Outer third
        |
        | New paragraph""".trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        Ol(listOf(
                            Li(listOf(P(listOf(Text(body = "Outer first\nOuter next line"))))),
                            Li(listOf(P(listOf(Text(body = "Outer second"))))),
                            Ol(listOf(
                                Li(listOf(P(listOf(Text(body = "Middle first\nMiddle next line"))))),
                                Li(listOf(P(listOf(Text(body = "Middle second"))))),
                                Ol(listOf(
                                    Li(listOf(P(listOf(Text(body = "Inner first\nInner next line")))))
                                    ),
                                    mapOf("start" to "1")
                                ),
                                Li(listOf(P(listOf(Text(body = "Middle third")))))
                                ),
                                mapOf("start" to "1")
                            ),
                            Li(listOf(P(listOf(Text(body = "Outer third")))))
                            ),
                            mapOf("start" to "1")
                        ),
                        P(listOf(Text(body = "New paragraph")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Ordered nested in Unordered nested in Ordered list`() {
        val kdoc = """
        | 1. Outer first
        | Outer next line
        | 2. Outer second
        |     + Middle first
        | Middle next line
        |     + Middle second
        |         1. Inner first
        | Inner next line
        |     + Middle third
        | 4. Outer third
        |
        | New paragraph""".trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        Ol(listOf(
                            Li(listOf(P(listOf(Text(body = "Outer first\nOuter next line"))))),
                            Li(listOf(P(listOf(Text(body = "Outer second"))))),
                            Ul(listOf(
                                Li(listOf(P(listOf(Text(body = "Middle first\nMiddle next line"))))),
                                Li(listOf(P(listOf(Text(body = "Middle second"))))),
                                Ol(listOf(
                                    Li(listOf(P(listOf(Text(body = "Inner first\nInner next line")))))
                                ),
                                    mapOf("start" to "1")
                                ),
                                Li(listOf(P(listOf(Text(body = "Middle third")))))
                            )),
                            Li(listOf(P(listOf(Text(body = "Outer third")))))
                        ),
                            mapOf("start" to "1")
                        ),
                        P(listOf(Text(body = "New paragraph")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Header and two paragraphs`() {
        val kdoc = """
        | # Header 1
        | Following text
        |
        | New paragraph
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        H1(listOf(Text(body = "Header 1"))),
                        P(listOf(Text(body = "Following text"))),
                        P(listOf(Text(body = "New paragraph")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Ignore //TODO: ATX_2 to ATX_6 and sometimes ATX_1 from jetbrains parser consumes white space. Need to handle it in their library
    @Test fun `All headers`() {
        val kdoc = """
        | # Header 1
        | Text 1
        | ## Header 2
        | Text 2
        | ### Header 3
        | Text 3
        | #### Header 4
        | Text 4
        | ##### Header 5
        | Text 5
        | ###### Header 6
        | Text 6
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        H1(listOf(Text(body = "Header 1"))),
                        P(listOf(Text(body = "Text 1"))),
                        H2(listOf(Text(body = "Header 2"))),
                        P(listOf(Text(body = "Text 2"))),
                        H3(listOf(Text(body = "Header 3"))),
                        P(listOf(Text(body = "Text 3"))),
                        H4(listOf(Text(body = "Header 4"))),
                        P(listOf(Text(body = "Text 4"))),
                        H5(listOf(Text(body = "Header 5"))),
                        P(listOf(Text(body = "Text 5"))),
                        H6(listOf(Text(body = "Header 6"))),
                        P(listOf(Text(body = "Text 6")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Bold New Line Bold`() {
        val kdoc = """
        | **line 1**
        | **line 2**
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        B(listOf(Text(body = "line 1"))),
                        Text(body = "\n"),
                        B(listOf(Text(body = "line 2")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Horizontal rule`() {
        val kdoc = """
        | ***
        | text 1
        | ___
        | text 2
        | ***
        | text 3
        | ___
        | text 4
        | ***
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        HorizontalRule,
                        P(listOf(Text(body = "text 1"))),
                        HorizontalRule,
                        P(listOf(Text(body = "text 2"))),
                        HorizontalRule,
                        P(listOf(Text(body = "text 3"))),
                        HorizontalRule,
                        P(listOf(Text(body = "text 4"))),
                        HorizontalRule
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Blockquote`() {
        val kdoc = """
        | > Blockquotes are very handy in email to emulate reply text.
        | > This line is part of the same quote.
        |
        | Quote break.
        |
        | > Quote
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        BlockQuote(listOf(
                            P(listOf(Text(body = "Blockquotes are very handy in email to emulate reply text.\nThis line is part of the same quote.")))
                        )),
                        P(listOf(Text(body = "Quote break."))),
                        BlockQuote(listOf(
                            P(listOf(Text(body = "Quote")))
                        ))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test fun `Blockquote nested`() {
        val kdoc = """
        | > text 1
        | > text 2
        | >> text 3
        | >> text 4
        | >
        | > text 5
        |
        | Quote break.
        |
        | > Quote
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        BlockQuote(listOf(
                            P(listOf(Text(body = "text 1\ntext 2"))),
                            BlockQuote(listOf(
                                P(listOf(Text(body = "text 3\ntext 4")))
                            )),
                            P(listOf(Text(body = "text 5")))
                            )),
                        P(listOf(Text(body = "Quote break."))),
                        BlockQuote(listOf(
                            P(listOf(Text(body = "Quote")))
                        ))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Ignore //TODO: Again ATX_1 consumes white space
    @Test fun `Blockquote nested with fancy text enhancement`() {
        val kdoc = """
        | > text **1**
        | > text 2
        | >> # text 3
        | >> * text 4
        | >>     * text 5
        | >
        | > text 6
        |
        | Quote break.
        |
        | > Quote
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        BlockQuote(listOf(
                            P(listOf(
                                Text(body = "text "),
                                B(listOf(Text(body = "1"))),
                                Text(body = "\ntext 2")
                            )),
                            BlockQuote(listOf(
                                H1(listOf(Text(body = "text 3"))),
                                Ul(listOf(
                                    Li(listOf(P(listOf(Text(body = "text 4"))))),
                                    Ul(listOf(
                                        Li(listOf(P(listOf(Text(body = "text 5")))))
                                    )
                                )))
                            )),
                            P(listOf(Text(body = "text 6")))
                        )),
                        P(listOf(Text(body = "Quote break."))),
                        BlockQuote(listOf(
                            P(listOf(Text(body = "Quote")))
                        ))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Simple Code Block`() {
        val kdoc = """
        | `Some code`
        | Sample text
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        Code(listOf(Text(body = "Some code"))),
                        Text(body = "\nSample text")
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Multilined Code Block`() {
        val kdoc = """
        | ```kotlin
        | val x: Int = 0
        | val y: String = "Text"
        |
        |     val z: Boolean = true
        | for(i in 0..10) {
        |     println(i)
        | }
        | ```
        | Sample text
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        Code(
                            listOf(
                                Text(body = "val x: Int = 0\nval y: String = \"Text\"\n\n    val z: Boolean = true\n" +
                                "for(i in 0..10) {\n    println(i)\n}")
                            ),
                            mapOf("lang" to "kotlin")
                        ),
                        P(listOf(Text(body = "Sample text")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test fun `Inline link`() {
        val kdoc = """
        | [I'm an inline-style link](https://www.google.com)
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(A(
                        listOf(Text(body = "I'm an inline-style link")),
                        mapOf("href" to "https://www.google.com")
                    )))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Inline link with title`() {
        val kdoc = """
        | [I'm an inline-style link with title](https://www.google.com "Google's Homepage")
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(A(
                        listOf(Text(body = "I'm an inline-style link with title")),
                        mapOf("href" to "https://www.google.com", "title" to "Google's Homepage")
                    )))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Full reference link`() {
        val kdoc = """
        | [I'm a reference-style link][Arbitrary case-insensitive reference text]
        |
        | [arbitrary case-insensitive reference text]: https://www.mozilla.org
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(P(listOf(A(
                        listOf(Text(body = "I'm a reference-style link")),
                        mapOf("href" to "https://www.mozilla.org")
                    )))))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Full reference link with number`() {
        val kdoc = """
        | [You can use numbers for reference-style link definitions][1]
        |
        | [1]: http://slashdot.org
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(P(listOf(A(
                        listOf(Text(body = "You can use numbers for reference-style link definitions")),
                        mapOf("href" to "http://slashdot.org")
                    )))))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Short reference link`() {
        val kdoc = """
        | Or leave it empty and use the [link text itself].
        |
        | [link text itself]: http://www.reddit.com
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(P(listOf(
                        Text(body = "Or leave it empty and use the "),
                        A(
                            listOf(Text(body = "link text itself")),
                            mapOf("href" to "http://www.reddit.com")
                        ),
                        Text(body = ".")
                    ))))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Autolink`() {
        val kdoc = """
        | URLs and URLs in angle brackets will automatically get turned into links.
        | http://www.example.com or <http://www.example.com> and sometimes
        | example.com (but not on Github, for example).
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        Text(body = "URLs and URLs in angle brackets will automatically get turned into links.\nhttp://www.example.com or "),
                        A(
                            listOf(Text(body = "http://www.example.com")),
                            mapOf("href" to "http://www.example.com")
                        ),
                        Text(body = " and sometimes\nexample.com (but not on Github, for example).")
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Various links`() {
        val kdoc = """
        | [I'm an inline-style link](https://www.google.com)
        |
        | [I'm an inline-style link with title](https://www.google.com "Google's Homepage")
        |
        | [I'm a reference-style link][Arbitrary case-insensitive reference text]
        |
        | [You can use numbers for reference-style link definitions][1]
        |
        | Or leave it empty and use the [link text itself].
        |
        | URLs and URLs in angle brackets will automatically get turned into links.
        | http://www.example.com or <http://www.example.com> and sometimes
        | example.com (but not on Github, for example).
        |
        | Some text to show that the reference links can follow later.
        |
        | [arbitrary case-insensitive reference text]: https://www.mozilla.org
        | [1]: http://slashdot.org
        | [link text itself]: http://www.reddit.com
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        P(listOf(A(
                            listOf(Text(body = "I'm an inline-style link")),
                            mapOf("href" to "https://www.google.com")
                        ))),
                        P(listOf(A(
                            listOf(Text(body = "I'm an inline-style link with title")),
                            mapOf("href" to "https://www.google.com", "title" to "Google's Homepage")
                        ))),
                        P(listOf(A(
                            listOf(Text(body = "I'm a reference-style link")),
                            mapOf("href" to "https://www.mozilla.org")
                        ))),
                        P(listOf(A(
                            listOf(Text(body = "You can use numbers for reference-style link definitions")),
                            mapOf("href" to "http://slashdot.org")
                        ))),
                        P(listOf(
                            Text(body = "Or leave it empty and use the "),
                            A(
                                listOf(Text(body = "link text itself")),
                                mapOf("href" to "http://www.reddit.com")
                            ),
                            Text(body = ".")
                        )),
                        P(listOf(
                            Text(body = "URLs and URLs in angle brackets will automatically get turned into links.\nhttp://www.example.com or "),
                            A(
                                listOf(Text(body = "http://www.example.com")),
                                mapOf("href" to "http://www.example.com")
                            ),
                            Text(body = " and sometimes\nexample.com (but not on Github, for example).")
                        )),
                        P(listOf(Text(body = "Some text to show that the reference links can follow later.")))
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test fun `Windows Carriage Return Line Feed`() {
        val kdoc = "text\r\ntext"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    P(listOf(
                        Text(body = "text\ntext")
                    ))
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }
}

