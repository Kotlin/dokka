package markdown

import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Test

class GfmSpecTest : KDocTest() {

    /* TODO: 1 .. 337 */

    /**
     * 6.3 Code spans
     *
     * A backtick string is a string of one or more backtick characters (`) that is
     * neither preceded nor followed by a backtick.
     *
     * A code span begins with a backtick string and ends with a backtick string of
     * equal length. The contents of the code span are the characters between the two
     * backtick strings, normalized in the following ways:
     *
     * - First, line endings are converted to spaces.
     * - If the resulting string both begins and ends with a space character, but does
     *   not consist entirely of space characters, a single space character is removed
     *   from the front and back. This allows you to include code that begins or ends
     *   with backtick characters, which must be separated by whitespace from the
     *   opening or closing backtick strings. */

    @Test /** This is a simple code span: */
    fun testCodeSpans_Example338() = testInlines(
        markdown = "`foo`",
        expected = listOf(
            CodeInline(listOf(Text("foo"))),
        )
    )

    @Test /** Here two backticks are used, because the code contains a backtick.
           *  This example also illustrates stripping of a single leading and trailing space: */
    fun testCodeSpans_Example339() = testInlines(
        markdown = "`` foo ` bar ``",
        expected = listOf(
            CodeInline(listOf(Text("` foo ` bar `"))), ///"foo ` bar"
        )
    )

    @Test /** This example shows the motivation for stripping leading and trailing spaces: */
    fun testCodeSpans_Example340() = testInlines(
        markdown = "` `` `",
        expected = listOf(
            CodeInline(listOf(Text("`` "))), ///"``"
        )
    )

    @Test /** Note that only _one_ space is stripped: */
    fun testCodeSpans_Example341() = testInlines(
        markdown = "`  ``  `",
        expected = listOf(
            CodeInline(listOf(Text("``  "))), ///" `` "
        )
    )

    @Test /** The stripping only happens if the space is on both sides of the string: */
    fun testCodeSpans_Example342() = testInlines(
        markdown = "` a`",
        expected = listOf(
            CodeInline(listOf(Text("a"))), ///" a"
        )
    )

    @Test /** Only spaces, and not unicode whitespace in general, are stripped in this way: */
    fun testCodeSpans_Example343() = testInlines(
        markdown = "` b `",
        expected = listOf(
            CodeInline(listOf(Text("b"), Text(" ", params = mapOf("content-type" to "html")))), ///" b "
        )
    )

    @Test /** No stripping occurs if the code span contains only spaces: */
    fun testCodeSpans_Example344() = testInlines(
        markdown = "` ` `  `",
        expected = listOf(
            CodeInline(), ///" "
            ///Text(" "),
            CodeInline(), ///"  "
        )
    )

    @Test /** Line endings are treated like spaces: */
    fun testCodeSpans_Example345() = testInlines(
        markdown = "``\nfoo\nbar  \nbaz\n``",
        expected = listOf(
            CodeInline(listOf(Text("` foo bar baz `"))), ///"foo bar   baz"
        )
    )

    @Test
    fun testCodeSpans_Example346() = testInlines(
        markdown = "``\nfoo \n``",
        expected = listOf(
            CodeInline(listOf(Text("` foo `"))), ///"foo "
        )
    )

    @Test /** Interior spaces are not collapsed: */
    fun testCodeSpans_Example347() = testInlines(
        markdown = "`foo   bar \nbaz`",
        expected = listOf(
            CodeInline(listOf(Text("foo   bar baz"))), ///"foo   bar  baz"
        )
    )

    /** Note that browsers will typically collapse consecutive spaces when rendering
     * <code> elements, so it is recommended that the following CSS be used:
     *
     * code{white-space: pre-wrap;}
     *
     * Note that backslash escapes do not work in code spans. */

    @Test /* All backslashes are treated literally: */
    fun testCodeSpans_Example348() = testInlines(
        markdown = "`foo\\`bar`",
        expected = listOf(
            CodeInline(listOf(Text("foo\\"))),
            Text("bar`"),
        )
    )

    @Test /** Backslash escapes are never needed, because one can always choose a string
           *  of n backtick characters as delimiters, where the code does not contain any
           *  strings of exactly n backtick characters. */
    fun testCodeSpans_Example349() = testInlines(
        markdown = "``foo`bar``",
        expected = listOf(
            CodeInline(listOf(Text("`foo`bar`"))), ///"foo`bar"
        )
    )

    @Test
    fun testCodeSpans_Example350() = testInlines(
        markdown = "` foo `` bar `",
        expected = listOf(
            CodeInline(listOf(Text("foo `` bar "))), ///"foo `` bar"
        )
    )

    @Test /** Code span backticks have higher precedence than any other inline constructs
           * except HTML tags and autolinks. Thus, for example, this is not parsed as
           * emphasized text, since the second * is part of a code span: */
    fun testCodeSpans_Example351() = testInlines(
        markdown = "*foo`*`",
        expected = listOf(
            Text("foo"), ///"*foo"
            CodeInline(listOf(Text("*"))),
        )
    )

    @Test /** And this is not parsed as a link: */
    fun testCodeSpans_Example352() = testInlines(
        markdown = "[not a `link](/foo`)",
        expected = listOf(
            Text("[not a "),
            CodeInline(listOf(Text("link](/foo"))),
            Text(")"),
        )
    )

    @Test /** Code spans, HTML tags, and autolinks have the same precedence.
           * Thus, this is code: */
    fun testCodeSpans_Example353() = testInlines(
        markdown = "`<a href=\"`\">`",
        expected = listOf(
            CodeInline(),
            ///CodeInline(listOf(Text("<a href=\""))),
            ///Text("\">`"),
        )
    )

    @Test /** But this is an HTML tag: */
    fun testCodeSpans_Example354() = testInlines(
        markdown = "<a href=\"`\">`",
        expected = listOf(
            Text("<a href=\"`\">", params = mapOf("content-type" to "html")),
            Text("`"),
        )
    )

    @Test /** And this is code: */
    fun testCodeSpans_Example355() = testInlines(
        markdown = "`<http://foo.bar.`baz>`",
        expected = listOf(
            CodeInline(),
            ///CodeInline(listOf(Text("<http://foo.bar."))),
            ///Text("baz>`"),
        )
    )

    @Test /** But this is an autolink: */
    fun testCodeSpans_Example356() = testInlines(
        markdown = "<http://foo.bar.`baz>`",
        expected = listOf(
            A(listOf(Text("http://foo.bar.`baz")), mapOf("href" to "http://foo.bar.`baz")),
            Text("`"),
        )
    )

    @Test /** When a backtick string is not closed by a matching backtick string,
           * we just have literal backticks: */
    fun testCodeSpans_Example357() = testInlines(
        markdown = "```foo``",
        expected = listOf(
            Text("```foo``"),
        )
    )

    @Test
    fun testCodeSpans_Example358() = testInlines(
        markdown = "`foo",
        expected = listOf(
            Text("`foo"),
        )
    )

    @Test /** The following case also illustrates the need for opening
           * and closing backtick strings to be equal in length: */
    fun testCodeSpans_Example359() = testInlines(
        markdown = "`foo``bar``",
        expected = listOf(
            Text("`foo"),
            CodeInline(listOf(Text("`bar`"))), ///"bar"
        )
    )

    /* TODO: 360 .. 673 */

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
}
