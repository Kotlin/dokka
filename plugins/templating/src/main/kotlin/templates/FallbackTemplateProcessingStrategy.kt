package org.jetbrains.dokka.templates

import org.jetbrains.dokka.DokkaConfiguration
import java.io.File

class FallbackTemplateProcessingStrategy : TemplateProcessingStrategy {

    override fun process(input: File, output: File, moduleContext: DokkaConfiguration.DokkaModuleDescription?): Boolean {
        if (input != output) input.copyTo(output, overwrite = true)
        return true
    }
}
