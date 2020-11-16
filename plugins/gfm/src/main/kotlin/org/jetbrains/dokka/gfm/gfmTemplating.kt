package org.jetbrains.dokka.gfm

import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.links.DRI
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS

@JsonTypeInfo(use = CLASS)
sealed class GfmCommand {
    companion object {
        private const val delimiter = "\u1680"
        fun templateCommand(command: GfmCommand): String = "$delimiter GfmCommand ${toJsonString(command)}$delimiter"
        val templateCommandRegex: Regex = Regex("$delimiter GfmCommand ([^$delimiter ]*)$delimiter")
    }
}

class ResolveLinkGfmCommand(val dri: DRI): GfmCommand()


