package org.jetbrains.dokka.templates

import org.jetbrains.dokka.Timer
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

class TestTemplatingGeneration(context: DokkaContext): Generation {

    val templatingPlugin by lazy { context.plugin<TemplatingPlugin>() }

    override fun Timer.generate() {
        report("Processing submodules")
        processSubmodules()
    }

    fun processSubmodules() =
        templatingPlugin.querySingle { templateProcessor }.process()

    override val generationName = "test template generation"
}