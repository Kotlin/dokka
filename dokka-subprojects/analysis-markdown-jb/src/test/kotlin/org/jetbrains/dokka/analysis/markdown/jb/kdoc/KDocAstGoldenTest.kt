/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.parser.CancellationToken
import kotlin.test.Test
import kotlin.test.assertEquals
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

/**
 * Golden tests for the raw markdown AST produced by [KDocFlavourDescriptor], i.e. the tree
 * [org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser.parseKDocComment] itself walks, before it's turned into
 * [org.jetbrains.dokka.model.doc.DocTag]s. Unlike [KDocSectionSplittingTest] (which only asserts on the final
 * [org.jetbrains.dokka.model.doc.DocTag] model), these pin the exact tree *shape* - in particular:
 *  - each `@tag` becomes a [KDocTokenTypes.KDOC_SECTION] composite node containing real nested block children
 *    (paragraphs, lists, ...), rather than a flat run of top-level siblings;
 *  - the tag name/subject are their own [KDocTokenTypes.KDOC_TAG_NAME]/[KDocTokenTypes.KDOC_TAG_SUBJECT] leaves,
 *    never part of a [org.intellij.markdown.MarkdownElementTypes.PARAGRAPH]'s own text.
 */
class KDocAstGoldenTest {

    private fun dump(text: String): String {
        val root = IntellijMarkdownParser(
            flavour = KDocFlavourDescriptor(),
            cancellationToken = CancellationToken.NonCancellable
        ).buildMarkdownTreeFromString(text as CharSequence)
        return dumpAst(root, text).trimEnd('\n')
    }

    private fun assertAst(expected: String, text: String) {
        assertEquals(expected.trimIndent(), dump(text))
    }

    @Test
    fun `multiple tags become sibling KDOC_SECTION nodes with the tag name as its own leaf`() {
        assertAst(
            """
            Markdown:MARKDOWN_FILE
              Markdown:PARAGRAPH
                Markdown:TEXT "Leading description."
              Markdown:EOL "\n"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@param"
                WHITE_SPACE " "
                Markdown:KDOC_TAG_SUBJECT "foo"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "the foo param"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@return"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "the result"
            """,
            "Leading description.\n\n@param foo the foo param\n@return the result"
        )
    }

    @Test
    fun `tag body paragraph continues across lines and a following list nests inside the same section`() {
        assertAst(
            """
            Markdown:MARKDOWN_FILE
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@param"
                WHITE_SPACE " "
                Markdown:KDOC_TAG_SUBJECT "foo"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "line one"
                  Markdown:EOL "\n"
                  Markdown:TEXT "line two continuing the same paragraph"
                Markdown:EOL "\n"
                Markdown:EOL "\n"
                Markdown:UNORDERED_LIST
                  Markdown:LIST_ITEM
                    Markdown:LIST_BULLET "- "
                    Markdown:PARAGRAPH
                      Markdown:TEXT "a list item"
                  Markdown:EOL "\n"
                  Markdown:LIST_ITEM
                    Markdown:LIST_BULLET "- "
                    Markdown:PARAGRAPH
                      Markdown:TEXT "another list item"
            """,
            "@param foo line one\nline two continuing the same paragraph\n\n- a list item\n- another list item"
        )
    }

    @Test
    fun `bracketed subject becomes its own leaf, excluding the brackets`() {
        assertAst(
            """
            Markdown:MARKDOWN_FILE
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@param"
                WHITE_SPACE "["
                Markdown:KDOC_TAG_SUBJECT "foo"
                WHITE_SPACE "] "
                Markdown:PARAGRAPH
                  Markdown:TEXT "text here"
            """,
            "@param[foo] text here"
        )
    }

    @Test
    fun `rich comment with multiple param tags, inline formatting, and cross references`() {
        assertAst(
            """
            Markdown:MARKDOWN_FILE
              Markdown:PARAGRAPH
                Markdown:TEXT "Computes the sum of two numbers."
              Markdown:EOL "\n"
              Markdown:EOL "\n"
              Markdown:PARAGRAPH
                Markdown:TEXT "This function is pure and has no side effects."
              Markdown:EOL "\n"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@param"
                WHITE_SPACE " "
                Markdown:KDOC_TAG_SUBJECT "a"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "the first"
                  WHITE_SPACE " "
                  Markdown:EMPH
                    Markdown:EMPH "*"
                    Markdown:TEXT "number"
                    Markdown:EMPH "*"
                  WHITE_SPACE " "
                  Markdown:TEXT "to add"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@param"
                WHITE_SPACE " "
                Markdown:KDOC_TAG_SUBJECT "b"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "the second number to add"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@param"
                WHITE_SPACE " "
                Markdown:KDOC_TAG_SUBJECT "base"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "the numeric base,"
                  WHITE_SPACE " "
                  Markdown:TEXT "must be"
                  WHITE_SPACE " "
                  Markdown:CODE_SPAN
                    Markdown:BACKTICK "`"
                    Markdown:TEXT "positive"
                    Markdown:BACKTICK "`"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@return"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "the sum of"
                  WHITE_SPACE " "
                  Markdown:SHORT_REFERENCE_LINK
                    Markdown:LINK_LABEL
                      Markdown:[ "["
                      Markdown:TEXT "a"
                      Markdown:] "]"
                  WHITE_SPACE " "
                  Markdown:TEXT "and"
                  WHITE_SPACE " "
                  Markdown:SHORT_REFERENCE_LINK
                    Markdown:LINK_LABEL
                      Markdown:[ "["
                      Markdown:TEXT "b"
                      Markdown:] "]"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@throws"
                WHITE_SPACE " "
                Markdown:KDOC_TAG_SUBJECT "IllegalArgumentException"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "if either argument is negative"
            """,
            """
            |Computes the sum of two numbers.
            |
            |This function is pure and has no side effects.
            |
            |@param a the first *number* to add
            |@param b the second number to add
            |@param base the numeric base, must be `positive`
            |@return the sum of [a] and [b]
            |@throws IllegalArgumentException if either argument is negative
            """.trimMargin()
        )
    }

    @Test
    fun `tag with no body is still its own KDOC_SECTION containing only the tag name`() {
        assertAst(
            """
            Markdown:MARKDOWN_FILE
              Markdown:PARAGRAPH
                Markdown:TEXT "Desc."
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@suppress"
            """,
            "Desc.\n@suppress"
        )
    }

    @Test
    fun `tag with no body immediately followed by another tag closes cleanly`() {
        assertAst(
            """
            Markdown:MARKDOWN_FILE
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@suppress"
              Markdown:EOL "\n"
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@return"
                WHITE_SPACE " "
                Markdown:PARAGRAPH
                  Markdown:TEXT "done"
            """,
            "@suppress\n@return done"
        )
    }

    @Test
    fun `body starting on the line after the tag is a normal paragraph with no leftover prefix`() {
        assertAst(
            """
            Markdown:MARKDOWN_FILE
              Markdown:KDOC_SECTION
                Markdown:KDOC_TAG_NAME "@param"
                WHITE_SPACE " "
                Markdown:KDOC_TAG_SUBJECT "foo"
                Markdown:EOL "\n"
                Markdown:PARAGRAPH
                  Markdown:TEXT "actual description here"
            """,
            "@param foo\nactual description here"
        )
    }
}

private fun dumpAst(root: ASTNode, text: String): String = buildString {
    fun visit(node: ASTNode, indent: String) {
        append(indent).append(node.type)
        if (node.children.isEmpty()) {
            append(' ').append(text.substring(node.startOffset, node.endOffset).toGoldenLiteral())
        }
        append('\n')
        node.children.forEach { visit(it, "$indent  ") }
    }
    visit(root, "")
}

private fun String.toGoldenLiteral(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
