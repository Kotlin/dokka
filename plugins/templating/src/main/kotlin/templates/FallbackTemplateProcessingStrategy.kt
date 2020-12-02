package org.jetbrains.dokka.templates

import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.nio.file.Files

class FallbackTemplateProcessingStrategy(dokkaContext: DokkaContext) : TemplateProcessingStrategy {

    override fun process(input: File, output: File): Boolean {
        Files.copy(input.toPath(), output.toPath())
        return true
    }
}