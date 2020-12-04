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
import java.io.BufferedWriter
import java.io.File

class GfmTemplateProcessingStrategy(context: DokkaContext) : TemplateProcessingStrategy {

    private val externalModuleLinkResolver =
        context.plugin<AllModulesPagePlugin>().querySingle { externalModuleLinkResolver }

    override suspend fun process(input: File, output: File): Boolean = coroutineScope {
        if (input.extension == "md") {
            launch(IO) {
                input.bufferedReader().use { reader ->
                    //This should also work whenever we have a misconfigured dokka and output is pointing to the input
                    //the same way that html processing does
                    if (input.absolutePath == output.absolutePath) {
                        val lines = reader.readLines()
                        output.bufferedWriter().use { writer ->
                            lines.forEach { line ->
                                writer.processAndWrite(line, output)
                            }

                        }
                    } else {
                        output.bufferedWriter().use { writer ->
                            reader.lineSequence().forEach { line ->
                                writer.processAndWrite(line, output)
                            }
                        }
                    }
                }
            }
            true
        } else false
    }

    private fun BufferedWriter.processAndWrite(line: String, output: File) =
        processLine(line, output).run {
            write(this)
            newLine()
        }

    private fun processLine(line: String, output: File): String =
        line.replace(templateCommandRegex) {
            when (val command = parseJson<GfmCommand>(it.command)) {
                is ResolveLinkGfmCommand -> resolveLink(output, command.dri, it.label)
            }
        }

    private fun resolveLink(fileContext: File, dri: DRI, label: String): String =
        externalModuleLinkResolver.resolve(dri, fileContext)?.let { address ->
            "[$label]($address)"
        } ?: label
}