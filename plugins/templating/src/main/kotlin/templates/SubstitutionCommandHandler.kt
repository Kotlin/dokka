/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.SubstitutionCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.File

public class SubstitutionCommandHandler(context: DokkaContext) : CommandHandler {

    override fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) {
        command as SubstitutionCommand
        val childrenCopy = body.children().toList()
        substitute(childrenCopy, TemplatingContext(input, output, childrenCopy, command))

        val position = body.elementSiblingIndex()
        val parent = body.parent()
        body.remove()

        parent?.insertChildren(position, childrenCopy)
    }

    override fun handleCommandAsComment(command: Command, body: List<Node>, input: File, output: File) {
        command as SubstitutionCommand
        substitute(body, TemplatingContext(input, output, body, command))
    }

    override fun canHandle(command: Command): Boolean = command is SubstitutionCommand

    override fun finish(output: File) { }

    private val substitutors = context.plugin<TemplatingPlugin>().query { substitutor }

    private fun findSubstitution(commandContext: TemplatingContext<SubstitutionCommand>, match: MatchResult): String =
        substitutors.asSequence().mapNotNull { it.trySubstitute(commandContext, match) }.firstOrNull() ?: match.value

    private fun substitute(elements: List<Node>, commandContext: TemplatingContext<SubstitutionCommand>) {
        val regex = commandContext.command.pattern.toRegex()
        elements.forEach { it.traverseToSubstitute(regex, commandContext) }
    }

    private fun Node.traverseToSubstitute(regex: Regex, commandContext: TemplatingContext<SubstitutionCommand>) {
        when (this) {
            is TextNode -> replaceWith(TextNode(wholeText.substitute(regex, commandContext)))
            is DataNode -> replaceWith(DataNode(wholeData.substitute(regex, commandContext)))
            is Element -> {
                attributes().forEach { attr(it.key, it.value.substitute(regex, commandContext)) }
                childNodes().forEach { it.traverseToSubstitute(regex, commandContext) }
            }
        }
    }

    private fun String.substitute(regex: Regex, commandContext: TemplatingContext<SubstitutionCommand>) = buildString {
        var lastOffset = 0
        regex.findAll(this@substitute).forEach { match ->
            append(this@substitute, lastOffset, match.range.first)
            append(findSubstitution(commandContext, match))
            lastOffset = match.range.last + 1
        }
        append(this@substitute, lastOffset, this@substitute.length)
    }
}
