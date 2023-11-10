/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.Command
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.io.File


public interface CommandHandler  {
    @Deprecated("This was renamed to handleCommandAsTag", ReplaceWith("handleCommandAsTag(command, element, input, output)"))
    public fun handleCommand(element: Element, command: Command, input: File, output: File) { }

    @Suppress("DEPRECATION")
    public fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) {
        handleCommand(body, command, input, output)
    }
    public fun handleCommandAsComment(command: Command, body: List<Node>, input: File, output: File) { }
    public fun canHandle(command: Command): Boolean
    public fun finish(output: File) {}
}

