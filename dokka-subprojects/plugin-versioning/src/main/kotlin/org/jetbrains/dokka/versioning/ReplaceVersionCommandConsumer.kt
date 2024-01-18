/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.versioning

import kotlinx.html.unsafe
import kotlinx.html.visit
import kotlinx.html.visitAndFinalize
import org.jetbrains.dokka.base.renderers.html.TemplateBlock
import org.jetbrains.dokka.base.renderers.html.command.consumers.ImmediateResolutionTagConsumer
import org.jetbrains.dokka.base.renderers.html.templateCommandFor
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.templating.ReplaceVersionsCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

public class ReplaceVersionCommandConsumer(context: DokkaContext) : ImmediateHtmlCommandConsumer {

    private val versionsNavigationCreator =
        context.plugin<VersioningPlugin>().querySingle { versionsNavigationCreator }
    private val versioningStorage =
        context.plugin<VersioningPlugin>().querySingle { versioningStorage }

    override fun canProcess(command: Command): Boolean = command is ReplaceVersionsCommand

    override fun <R> processCommand(
        command: Command,
        block: TemplateBlock,
        tagConsumer: ImmediateResolutionTagConsumer<R>
    ) {
        command as ReplaceVersionsCommand
        templateCommandFor(command, tagConsumer).visit {
            unsafe {
                +versionsNavigationCreator(versioningStorage.currentVersion.dir.resolve(command.location))
            }
        }
    }

    override fun <R> processCommandAndFinalize(
        command: Command,
        block: TemplateBlock,
        tagConsumer: ImmediateResolutionTagConsumer<R>
    ): R {
        command as ReplaceVersionsCommand
        return templateCommandFor(command, tagConsumer).visitAndFinalize(tagConsumer) {
            unsafe {
                +versionsNavigationCreator(versioningStorage.currentVersion.dir.resolve(command.location))
            }
        }
    }
}
