package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.Command
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.io.File


interface CommandHandler  {
    @Deprecated("This was renamed to handleCommandAsTag", ReplaceWith("handleCommandAsTag(command, element, input, output)"))
    fun handleCommand(element: Element, command: Command, input: File, output: File) { }

    @Suppress("DEPRECATION")
    fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) =
        handleCommand(body, command, input, output)
    fun handleCommandAsComment(command: Command, body: List<Node>, input: File, output: File) { }
    fun canHandle(command: Command): Boolean
    fun finish(output: File) {}
}