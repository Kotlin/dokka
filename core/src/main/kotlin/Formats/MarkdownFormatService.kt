package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName
import java.util.*

enum class ListKind {
    Ordered,
    Unordered
}

private class ListState(val kind: ListKind, var size: Int = 1) {
    fun getTagAndIncrement() = when (kind) {
        ListKind.Ordered -> "${size++}. "
        else -> "* "
    }
}

private val TWO_LINE_BREAKS = System.lineSeparator() + System.lineSeparator()

open class MarkdownOutputBuilder(to: StringBuilder,
                                 location: Location,
                                 generator: NodeLocationAwareGenerator,
                                 languageService: LanguageService,
                                 extension: String,
                                 impliedPlatforms: List<String>)
    : StructuredOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)
{
    private val listStack = ArrayDeque<ListState>()
    protected var inTableCell = false
    protected var inCodeBlock = false
    private var lastTableCellStart = -1
    private var maxBackticksInCodeBlock = 0

    private fun appendNewline() {
        while (to.endsWith(' ')) {
            to.setLength(to.length - 1)
        }
        to.appendln()
    }

    private fun ensureNewline() {
        if (inTableCell && listStack.isEmpty()) {
            if (to.length != lastTableCellStart && !to.endsWith("<br>")) {
                to.append("<br>")
            }
        }
        else {
            if (!endsWithNewline()) {
                appendNewline()
            }
        }
    }

    private fun endsWithNewline(): Boolean {
        var index = to.length - 1
        while (index > 0) {
            val c = to[index]
            if (c != ' ') {
                return c == '\n'
            }
            index--
        }
        return false
    }

    override fun ensureParagraph() {
        if (!to.endsWith(TWO_LINE_BREAKS)) {
            if (!to.endsWith('\n')) {
                appendNewline()
            }
            appendNewline()
        }
    }
    override fun appendBreadcrumbSeparator() {
        to.append(" / ")
    }

    private val backTickFindingRegex = """(`+)""".toRegex()

    override fun appendText(text: String) {
        if (inCodeBlock) {
            to.append(text)
            val backTicks = backTickFindingRegex.findAll(text)
            val longestBackTickRun = backTicks.map { it.value.length }.max() ?: 0
            maxBackticksInCodeBlock = maxBackticksInCodeBlock.coerceAtLeast(longestBackTickRun)
        }
        else {
            if (text == "\n" && inTableCell) {
                to.append(" ")
            } else {
                to.append(text.htmlEscape())
            }
        }
    }

    override fun appendCode(body: () -> Unit) {
        inCodeBlock = true
        val codeBlockStart = to.length
        maxBackticksInCodeBlock = 0

        wrapIfNotEmpty("`", "`", body, checkEndsWith = true)

        if (maxBackticksInCodeBlock > 0) {
            val extraBackticks = "`".repeat(maxBackticksInCodeBlock)
            to.insert(codeBlockStart, extraBackticks)
            to.append(extraBackticks)
        }

        inCodeBlock = false
    }

    override fun appendUnorderedList(body: () -> Unit) {
        listStack.push(ListState(ListKind.Unordered))
        body()
        listStack.pop()
        ensureNewline()
    }

    override fun appendOrderedList(body: () -> Unit) {
        listStack.push(ListState(ListKind.Ordered))
        body()
        listStack.pop()
        ensureNewline()
    }

    override fun appendListItem(body: () -> Unit) {
        ensureNewline()
        to.append(listStack.peek()?.getTagAndIncrement())
        body()
        ensureNewline()
    }

    override fun appendStrong(body: () -> Unit) = wrap("**", "**", body)
    override fun appendEmphasis(body: () -> Unit) = wrap("*", "*", body)
    override fun appendStrikethrough(body: () -> Unit) = wrap("~~", "~~", body)

    override fun appendLink(href: String, body: () -> Unit) {
        if (inCodeBlock) {
            wrap("`[`", "`]($href)`", body)
        }
        else {
            wrap("[", "]($href)", body)
        }
    }

    override fun appendLine() {
        if (inTableCell) {
            to.append("<br>")
        }
        else {
            appendNewline()
        }
    }

    override fun appendAnchor(anchor: String) {
        // no anchors in Markdown
    }

    override fun appendParagraph(body: () -> Unit) {
        if (inTableCell) {
            ensureNewline()
            body()
        } else if (listStack.isNotEmpty()) {
            body()
            ensureNewline()
        } else {
            ensureParagraph()
            body()
            ensureParagraph()
        }
    }

    override fun appendHeader(level: Int, body: () -> Unit) {
        ensureParagraph()
        to.append("${"#".repeat(level)} ")
        body()
        ensureParagraph()
    }

    override fun appendBlockCode(language: String, body: () -> Unit) {
        inCodeBlock = true
        ensureParagraph()
        to.appendln(if (language.isEmpty()) "```" else "``` $language")
        body()
        ensureNewline()
        to.appendln("```")
        appendLine()
        inCodeBlock = false
    }

    override fun appendTable(vararg columns: String, body: () -> Unit) {
        ensureParagraph()
        body()
        ensureParagraph()
    }

    override fun appendTableBody(body: () -> Unit) {
        body()
    }

    override fun appendTableRow(body: () -> Unit) {
        to.append("|")
        body()
        appendNewline()
    }

    override fun appendTableCell(body: () -> Unit) {
        to.append(" ")
        inTableCell = true
        lastTableCellStart = to.length
        body()
        inTableCell = false
        to.append(" |")
    }

    override fun appendNonBreakingSpace() {
        if (inCodeBlock) {
            to.append(" ")
        }
        else {
            to.append("&nbsp;")
        }
    }
}

open class MarkdownFormatService(generator: NodeLocationAwareGenerator,
                                 signatureGenerator: LanguageService,
                                 linkExtension: String,
                                 val impliedPlatforms: List<String>)
: StructuredFormatService(generator, signatureGenerator, "md", linkExtension) {
    @Inject constructor(generator: NodeLocationAwareGenerator,
                        signatureGenerator: LanguageService,
                        @Named(impliedPlatformsName) impliedPlatforms: List<String>): this(generator, signatureGenerator, "md", impliedPlatforms)

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
        MarkdownOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)
}
