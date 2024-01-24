/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.versioning


import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ReplaceVersionsCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.templates.CommandHandler
import org.jsoup.nodes.Element
import java.io.File

public class ReplaceVersionCommandHandler(context: DokkaContext) : CommandHandler {

    public val versionsNavigationCreator: VersionsNavigationCreator by lazy {
        context.plugin<VersioningPlugin>().querySingle { versionsNavigationCreator }
    }

    override fun canHandle(command: Command): Boolean = command is ReplaceVersionsCommand

    override fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) {
        body.empty()
        body.append(versionsNavigationCreator(output))
    }
}
