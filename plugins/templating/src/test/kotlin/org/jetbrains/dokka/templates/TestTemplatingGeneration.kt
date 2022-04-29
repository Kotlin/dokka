package org.jetbrains.dokka.templates

import org.jetbrains.dokka.Timer
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle

class TestTemplatingGeneration(private val context: DokkaContext) : Generation {

    val templatingPlugin by lazy { context.plugin<TemplatingPlugin>() }

    override fun Timer.generate() {
        report("Processing submodules")
        processSubmodules()

        report("Finishing processing")
        finishProcessing()
    }

    fun processSubmodules() =
        templatingPlugin.querySingle { submoduleTemplateProcessor }.process(context.configuration.modules)

    fun finishProcessing() =
        templatingPlugin.query { templateProcessingStrategy }.forEach { it.finish(context.configuration.outputDir) }


    override val generationName = "test template generation"
}