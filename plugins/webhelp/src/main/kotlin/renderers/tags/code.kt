package org.jetbrains.dokka.webhelp.renderers.tags

import kotlinx.html.*

enum class CodeStyle {
    INLINE, BLOCK
}

//TODO, i don't know how to avoid strings here
@HtmlTagMarker
inline fun FlowContent.code(style: CodeStyle = CodeStyle.INLINE, lang: String, crossinline block : CODE.() -> Unit = {}) : Unit =
    code {
        attributes["style"] = style.toString().toLowerCase()
        attributes["lang"] = lang
        +"<![CDATA["
        block()
        +"]]>"
    }
