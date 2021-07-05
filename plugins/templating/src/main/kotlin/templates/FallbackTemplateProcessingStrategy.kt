package org.jetbrains.dokka.templates

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

class FallbackTemplateProcessingStrategy : TemplateProcessingStrategy {

    override fun process(input: File, output: File, moduleContext: DokkaConfiguration.DokkaModuleDescription?): Boolean {
        if (input != output) input.copyTo(output, overwrite = true)
        return true
    }
}
