/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ResolveLinkCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.templates.CommandHandler
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import java.io.File

public class ResolveLinkCommandHandler(context: DokkaContext) : CommandHandler {

    private val externalModuleLinkResolver =
        context.plugin<AllModulesPagePlugin>().querySingle { externalModuleLinkResolver }

    override fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) {
        command as ResolveLinkCommand
        val link = externalModuleLinkResolver.resolve(command.dri, output)
        if (link == null) {
            val children = body.childNodes().toList()
            val attributes = Attributes().apply {
                put("data-unresolved-link", command.dri.toString())
            }
            val el = Element(Tag.valueOf("span"), "", attributes).apply {
                children.forEach { ch -> appendChild(ch) }
            }
            body.replaceWith(el)
            return
        }

        val attributes = Attributes().apply {
            put("href", link)
        }
        val children = body.childNodes().toList()
        val el = Element(Tag.valueOf("a"), "", attributes).apply {
            children.forEach { ch -> appendChild(ch) }
        }
        body.replaceWith(el)
    }

    override fun canHandle(command: Command): Boolean = command is ResolveLinkCommand
}
