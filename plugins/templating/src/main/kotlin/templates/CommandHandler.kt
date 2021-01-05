package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.Command
import org.jsoup.nodes.Element
import java.io.File

interface CommandHandler {
    fun handleCommand(element: Element, command: Command, input: File, output: File)
    fun canHandle(command: Command): Boolean
    fun finish(output: File) {}
}