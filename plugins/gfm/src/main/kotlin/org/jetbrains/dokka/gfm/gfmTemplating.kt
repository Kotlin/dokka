package org.jetbrains.dokka.gfm

import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.links.DRI
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS

@JsonTypeInfo(use = CLASS)
sealed class GfmCommand {
    companion object {
        private const val delimiter = "\u1680"
        val templateCommandRegex: Regex =
            Regex("<!---$delimiter GfmCommand ([^$delimiter ]*)$delimiter--->(.+?)(?=<!---$delimiter)<!---$delimiter--->")
        val MatchResult.command
            get() = groupValues[1]
        val MatchResult.label
            get() = groupValues[2]
        fun Appendable.templateCommand(command: GfmCommand, content: Appendable.() -> Unit): Unit {
            append("<!---$delimiter GfmCommand ${toJsonString(command)}$delimiter--->")
            content()
            append("<!---$delimiter--->")
        }
    }
}

class ResolveLinkGfmCommand(val dri: DRI) : GfmCommand()


