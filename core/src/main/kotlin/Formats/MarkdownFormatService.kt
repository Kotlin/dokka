package org.jetbrains.dokka

import com.google.inject.Inject
import java.util.*


open class MarkdownFormatService(locationService: LocationService,
                                 signatureGenerator: LanguageService,
                                 linkExtension: String)
: StructuredFormatService(locationService, signatureGenerator, "md", linkExtension) {
    @Inject constructor(locationService: LocationService,
                        signatureGenerator: LanguageService): this(locationService, signatureGenerator, "md")

    override fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString(" / ")
    }

    private fun String.escapeTextAndCleanLf(): String {
       return (if (allowedLf().escapeText) this.htmlEscape() else this).adjustLf()
    }

    override fun formatText(text: String): String = text.escapeTextAndCleanLf()
    override fun formatSymbol(text: String): String = text.escapeTextAndCleanLf()
    override fun formatKeyword(text: String): String = text.escapeTextAndCleanLf()
    override fun formatIdentifier(text: String, kind: IdentifierKind, signature: String?): String = text.escapeTextAndCleanLf()

    /**
     * Make sure that if the stack has a more restrictive LF replacement, we don't undo it.  Therefore a priority order needs to be known
     */
    enum class LfPriority(val lf: String, val paraStart: String, val paraEnd: String, val requiredWhitespace: String, val nbsp: String, val escapeText: Boolean, val weight: Int) {
        SlashN("\n", "\n", "\n", "\n", "&nbsp;", true, 1),
        HtmlBr("<br/>", "<p>", "</p>", "", "&nbsp;", true, 2),
        Code("\n", "\n", "\n", "\n", "\u00A0", false, 2), // or \u00A0 for nbsp?
        Empty(" ", " ", " ", "", " ", true, 3)
    }

    private val lfStack: MutableList<LfPriority> = arrayListOf(LfPriority.SlashN)
    private fun pushLf(lf: LfPriority) = lfStack.add(lf)
    private fun popLf() = lfStack.removeAt(lfStack.lastIndex)
    private fun allowedLf(): LfPriority = lfStack.last()
    private fun allowedParentLf(): LfPriority = if (lfStack.size > 1) lfStack.takeLast(2).first() else allowedLf()

    private fun changeLf(newLf: LfPriority, block: ()->Unit) {
        val current = allowedLf()
        pushLf(if (newLf.weight >= current.weight) newLf else current)
        block()
        popLf()
    }

    private fun String.adjustLf(): String {
        val current = allowedLf()
        return if (current == LfPriority.SlashN) this else this.replace("\n", current.lf)
    }

    private fun StringBuilder.appendWithAdjustedLF(text: String = "") {
        this.append(text.adjustLf())
        this.append(allowedLf().lf)
    }

    private val backTickFindingRegex = """(`+)""".toRegex()

    override fun appendCode(to: StringBuilder, bodyAsText: ()->String) {
        changeLf(LfPriority.Code) {
            val code = bodyAsText()
            // if there is one or more backticks in the code, the fence must be one more back tick longer
            val backTicks = backTickFindingRegex.findAll(code)
            val longestBackTickRun = backTicks.map { it.value.length }.max() ?: 0
            val boundingTicks = "`".repeat(longestBackTickRun+1)
            to.append("$boundingTicks$code$boundingTicks")
        }
    }

    override fun appendAsSignature(to: StringBuilder, node: ContentNode, block: () -> Unit) {
        changeLf(LfPriority.HtmlBr) {
            block()
        }
    }

    override fun appendAsOverloadGroup(to: StringBuilder, block: () -> Unit) {
        changeLf(LfPriority.HtmlBr) {
            block()
        }
    }

    override fun formatUnorderedList(text: String): String {
        return text
    }
    override fun formatOrderedList(text: String): String {
        return text
    }

    override fun formatListItem(text: String, kind: ListKind): String {
        val itemText = if (text.endsWith(allowedParentLf().requiredWhitespace)) text else text + allowedParentLf().requiredWhitespace
        return if (kind == ListKind.Unordered) "* $itemText" else "1. $itemText"
    }

    override fun formatStrong(text: String): String {
        return "**$text**"
    }

    override fun formatEmphasis(text: String): String {
        return "*$text*"
    }

    override fun formatStrikethrough(text: String): String {
        return "~~$text~~"
    }

    override fun formatLink(text: String, href: String): String {
        return """[${text.replace("[", "\\[").replace("]","\\]")}](${href})"""
    }

    override fun appendLine(to: StringBuilder, text: String) {
        to.appendWithAdjustedLF(text)
    }

    override fun appendList(to: StringBuilder, body: () -> Unit) {
        to.append(allowedLf().requiredWhitespace)
        changeLf(LfPriority.HtmlBr) { body() }
    }

    override fun appendAnchor(to: StringBuilder, anchor: String) {
        // no anchors in Markdown
    }

    override fun appendParagraph(to: StringBuilder, text: String) {
        to.append(allowedLf().paraStart)
        to.append(text)
        to.append(allowedLf().paraEnd)
    }

    override fun appendHeader(to: StringBuilder, text: String, level: Int) {
        to.append(allowedLf().requiredWhitespace)
        to.appendWithAdjustedLF("${"#".repeat(level)} $text")
    }

    override fun appendBlockCode(to: StringBuilder, language: String, bodyAsLines: ()->List<String>) {
        to.append(allowedLf().requiredWhitespace)
        changeLf(LfPriority.Code) {
            // this does not adjust line feeds, a code block in a list or table will just break formatting
            to.appendln(if (language.isEmpty()) "```" else "``` $language")
            // use 4 zero width spaces which act as a code fence escaping any use of ``` within the code block
            to.appendln(bodyAsLines().map { "​\u200B\u200B\u200B\u200B$it" }.joinToString("\n"))
            to.appendln("```")
        }
    }

    override fun appendTable(to: StringBuilder, columnCount: Int, body: () -> Unit) {
        to.appendln("|" + "&nbsp;|".repeat(columnCount))
        to.appendln("|" + "---|".repeat(columnCount))
        body()
    }

    override fun appendTableBody(to: StringBuilder, body: () -> Unit) {
        body()
    }

    override fun appendTableRow(to: StringBuilder, body: () -> Unit) {
        to.append("|")
        body()
        to.appendln()
    }

    override fun appendTableCell(to: StringBuilder, body: () -> Unit) {
        to.append(" ")
        changeLf(LfPriority.HtmlBr) { body() }
        to.append(" |")
    }

    override fun formatNonBreakingSpace(): String = allowedLf().nbsp
}
