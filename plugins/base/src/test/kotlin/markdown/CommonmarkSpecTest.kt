package markdown

import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Test

class CommonmarkSpecTest : KDocTest() {

    private fun testInlines(markdown: String, expected: List<DocTag>) {
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(P(expected)),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(markdown, expectedDocumentationNode)
    }


    /**
     * ## Code spans
     *
     * A [backtick string](@)
     * is a string of one or more backtick characters (`` ` ``) that is neither
     * preceded nor followed by a backtick.
     *
     * A [code span](@) begins with a backtick string and ends with
     * a backtick string of equal length.  The contents of the code span are
     * the characters between these two backtick strings, normalized in the
     * following ways:
     *
     * - First, [line endings] are converted to [spaces].
     * - If the resulting string both begins *and* ends with a [space]
     *   character, but does not consist entirely of [space]
     *   characters, a single [space] character is removed from the
     *   front and back.  This allows you to include code that begins
     *   or ends with backtick characters, which must be separated by
     *   whitespace from the opening or closing backtick strings.
     *
     * This is a simple code span:
     */
    @Test fun codeSpans_Example1() = testInlines(
        markdown = "`foo`",
        expected = listOf(CodeInline(listOf(Text("foo"))))
    )

    /**
     * Here two backticks are used, because the code contains a backtick.
     * This example also illustrates stripping of a single leading and
     * trailing space:
     */
    @Test fun codeSpans_Example2() = testInlines(
        markdown = "`` foo ` bar ``",
        expected = listOf(CodeInline(listOf(Text("` foo ` bar `"))))
    /// expected = listOf(CodeInline(listOf(Text("foo ` bar"))))
    )

    /**
     * This example shows the motivation for stripping leading and trailing
     * spaces:
     */
    @Test fun codeSpans_Example3() = testInlines(
        markdown = "` `` `",
        expected = listOf(CodeInline(listOf(Text("`` "))))
    /// expected = listOf(CodeInline(listOf(Text("``"))))
    )

    /**
     * Note that only *one* space is stripped:
     */
    @Test fun codeSpans_Example4() = testInlines(
        markdown = "`  ``  `",
        expected = listOf( CodeInline(listOf(Text("``  "))))
    /// expected = listOf( CodeInline(listOf(Text(" `` "))))
    )

    /**
     * The stripping only happens if the space is on both
     * sides of the string:
     */
    @Test fun codeSpans_Example5() = testInlines(
        markdown = "` a`",
        expected = listOf(CodeInline(listOf(Text("a"))))
    /// expected = listOf(CodeInline(listOf(Text(" a"))))
    )

    /**
     * Only [spaces], and not [unicode whitespace] in general, are
     * stripped in this way:
     */
    @Test fun codeSpans_Example6() = testInlines(
        markdown = "` b `",
        expected = listOf(CodeInline(listOf(Text("b"), Text(" ", params = mapOf("content-type" to "html")))))
    /// expected = listOf(CodeInline(listOf(Text(" b "))))
    )

    /**
     * No stripping occurs if the code span contains only spaces:
     */
    @Test fun codeSpans_Example7() = testInlines(
        markdown = "` `\n`  `",
        expected = listOf(CodeInline(), CodeInline())
    /// expected = listOf(CodeInline(listOf(Text(" ")), Text("\n"), CodeInline(listOf(Text("  ")))
    )

    /**
     * [Line endings] are treated like spaces:
     */
    @Test fun codeSpans_Example8() = testInlines(
        markdown = "``\nfoo\nbar  \nbaz\n``",
        expected = listOf(CodeInline(listOf(Text("` foo bar baz `"))))
    /// expected = listOf(CodeInline(listOf(Text("foo bar   baz"))))
    )

    @Test fun codeSpans_Example9() = testInlines(
        markdown = "``\nfoo \n``",
        expected = listOf(CodeInline(listOf(Text("` foo `"))))
    /// expected = listOf(CodeInline(listOf(Text("foo "))))
    )

    /**
     * Interior spaces are not collapsed:
     */
    @Test fun codeSpans_Example10() = testInlines(
        markdown = "`foo   bar \nbaz`",
        expected = listOf(CodeInline(listOf(Text("foo   bar baz"))))
    /// expected = listOf(CodeInline(listOf(Text("foo   bar  baz"))))
    )

    /**
     * Note that browsers will typically collapse consecutive spaces
     * when rendering `<code>` elements, so it is recommended that
     * the following CSS be used:
     *
     *     code{white-space: pre-wrap;}
     *
     *
     * Note that backslash escapes do not work in code spans. All backslashes
     * are treated literally:
     */
    @Test fun codeSpans_Example11() = testInlines(
        markdown = "`foo\\`bar`",
        expected = listOf(CodeInline(listOf(Text("foo\\"))), Text("bar`"))
    )

    /**
     * Backslash escapes are never needed, because one can always choose a
     * string of *n* backtick characters as delimiters, where the code does
     * not contain any strings of exactly *n* backtick characters.
     */
    @Test fun codeSpans_Example12() = testInlines(
        markdown = "``foo`bar``",
        expected = listOf(CodeInline(listOf(Text("`foo`bar`"))))
    /// expected = listOf(CodeInline(listOf(Text("foo`bar"))))
    )

    @Test fun codeSpans_Example13() = testInlines(
        markdown = "` foo `` bar `",
        expected = listOf(CodeInline(listOf(Text("foo `` bar "))))
    /// expected = listOf(CodeInline(listOf(Text("foo `` bar"))))
    )

    /**
     * Code span backticks have higher precedence than any other inline
     * constructs except HTML tags and autolinks.  Thus, for example, this is
     * not parsed as emphasized text, since the second `*` is part of a code
     * span:
     */
    @Test fun codeSpans_Example14() = testInlines(
        markdown = "*foo`*`",
        expected = listOf(Text("foo"), CodeInline(listOf(Text("*"))))
    /// expected = listOf(Text("*foo"), CodeInline(listOf(Text("*"))))
    )

    /**
     * And this is not parsed as a link:
     */
    @Test fun codeSpans_Example15() = testInlines(
        markdown = "[not a `link](/foo`)",
        expected = listOf(Text("[not a "), CodeInline(listOf(Text("link](/foo"))), Text(")"))
    )

    /**
     * Code spans, HTML tags, and autolinks have the same precedence.
     * Thus, this is code:
     */
    @Test fun codeSpans_Example16() = testInlines(
        markdown = "`<a href=\"`\">`",
        expected = listOf(CodeInline())
    /// expected = listOf(CodeInline(listOf(Text("<a href=\""))), Text("\">`"))
    )

    /**
     * But this is an HTML tag:
     */
    @Test fun codeSpans_Example17() = testInlines(
        markdown = "<a href=\"`\">`",
        expected = listOf(Text("<a href=\"`\">", params = mapOf("content-type" to "html")), Text("`"))
    )

    /**
     * And this is code:
     */
    @Test fun codeSpans_Example18() = testInlines(
        markdown = "`<http://foo.bar.`baz>`",
        expected = listOf(CodeInline())
    /// expected = listOf(CodeInline(listOf(Text("<http://foo.bar."))), Text("baz>`"))
    )

    /**
     * But this is an autolink:
     */
    @Test fun codeSpans_Example19() = testInlines(
        markdown = "<http://foo.bar.`baz>`",
        expected = listOf(A(listOf(Text("http://foo.bar.`baz")), mapOf("href" to "http://foo.bar.`baz")), Text("`"))
    )

    /**
     * When a backtick string is not closed by a matching backtick string,
     * we just have literal backticks:
     */
    @Test fun codeSpans_Example20() = testInlines(
        markdown = "```foo``",
        expected = listOf(Text("```foo``"))
    )

    @Test fun codeSpans_Example21() = testInlines(
        markdown = "`foo",
        expected = listOf(Text("`foo"))
    )

    /**
     * The following case also illustrates the need for opening and
     * closing backtick strings to be equal in length:
     */
    @Test fun codeSpans_Example22() = testInlines(
        markdown = "`foo``bar``",
        expected = listOf(Text("`foo"), CodeInline(listOf(Text("`bar`"))))
    /// expected = listOf(Text("`foo"), CodeInline(listOf(Text("bar"))))
    )
}
