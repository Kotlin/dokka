/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gfm

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.links.DRI

@JsonTypeInfo(use = CLASS)
public sealed class GfmCommand {

    public companion object {
        private const val delimiter = "\u1680"

        public val templateCommandRegex: Regex =
            Regex("<!---$delimiter GfmCommand ([^$delimiter ]*)$delimiter--->(.+?)(?=<!---$delimiter)<!---$delimiter--->")

        public val MatchResult.command: String
            get() = groupValues[1]

        public val MatchResult.label: String
            get() = groupValues[2]

        public fun Appendable.templateCommand(command: GfmCommand, content: Appendable.() -> Unit) {
            append("<!---$delimiter GfmCommand ${toJsonString(command)}$delimiter--->")
            content()
            append("<!---$delimiter--->")
        }
    }
}

public class ResolveLinkGfmCommand(
    public val dri: DRI
) : GfmCommand()


