package org.jetbrains.dokka.gfm.templateProcessing

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.allModulesPage.templates.TemplateProcessingStrategy
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.gfm.GfmCommand
import org.jetbrains.dokka.gfm.GfmCommand.Companion.command
import org.jetbrains.dokka.gfm.GfmCommand.Companion.label
import org.jetbrains.dokka.gfm.GfmCommand.Companion.templateCommandRegex
import org.jetbrains.dokka.gfm.ResolveLinkGfmCommand
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import java.io.File

class GfmTemplateProcessingStrategy(context: DokkaContext) : TemplateProcessingStrategy {

    private val externalModuleLinkResolver = context.plugin<AllModulesPagePlugin>().querySingle { externalModuleLinkResolver }

    override suspend fun process(input: File, output: File): Boolean = coroutineScope {
        if (input.extension == "md") {
            launch(IO) {
                input.bufferedReader().use { reader ->
                    output.bufferedWriter().use { writer ->
                        do {
                            val line = reader.readLine()
                            if (line != null) {
                                writer.write(line.replace(templateCommandRegex) {
                                    when (val command = parseJson<GfmCommand>(it.command)) {
                                        is ResolveLinkGfmCommand -> resolveLink(output, command.dri, it.label)
                                    }
                                })
                                writer.newLine()
                            }
                        } while (line != null)
                    }
                }
            }
            true
        } else false
    }

    private fun resolveLink(fileContext: File, dri: DRI, label: String): String =
        externalModuleLinkResolver.resolve(dri, fileContext)?.let { address ->
            "[$label]($address)"
        } ?: label
}