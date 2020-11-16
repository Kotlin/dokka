package org.jetbrains.dokka.gfm.allModulesPage

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.dokka.allModulesPage.templates.ExternalModuleLinkResolver
import org.jetbrains.dokka.allModulesPage.templates.TemplateProcessingStrategy
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.gfm.GfmCommand
import org.jetbrains.dokka.gfm.GfmCommand.Companion.templateCommandRegex
import org.jetbrains.dokka.gfm.ResolveLinkGfmCommand
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

class GfmTemplateProcessingStrategy(val context: DokkaContext) : TemplateProcessingStrategy {

    private val externalModuleLinkResolver = ExternalModuleLinkResolver(context)

    override suspend fun process(input: File, output: File): Boolean = coroutineScope {
        if (input.extension == "md") {
            launch(IO) {
                val reader = input.bufferedReader()
                val writer = output.bufferedWriter()
                do {
                    val line = reader.readLine()
                    if (line != null) {
                        writer.write(line.replace(templateCommandRegex) {
                            when (val command = parseJson<GfmCommand>(it.groupValues.last())) {
                                is ResolveLinkGfmCommand -> resolveLink(output, command.dri)
                            }
                        })
                        writer.newLine()
                    }
                } while (line != null)
                reader.close()
                writer.close()
            }
            true
        } else false
    }

    private fun resolveLink(fileContext: File, dri: DRI): String =
        externalModuleLinkResolver.resolve(dri, fileContext) ?: ""
}