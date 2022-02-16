package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.Command
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.io.File

interface CommandHandler {
    fun handleCommand(element: Element, command: Command, input: File, output: File)
    fun canHandle(command: Command): Boolean
    fun finish(output: File) {}
}

interface CommentCommandHandler {
    fun handleCommand(nodes: List<Node>, command: Command, input: File, output: File)
    fun canHandle(command: Command): Boolean
    fun finish(output: File) {}
}