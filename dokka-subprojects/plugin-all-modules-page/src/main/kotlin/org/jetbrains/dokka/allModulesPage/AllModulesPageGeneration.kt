/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.Timer
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.templates.TemplatingPlugin
import org.jetbrains.dokka.templates.TemplatingResult
import org.jetbrains.dokka.transformers.pages.CreationContext

public class AllModulesPageGeneration(private val context: DokkaContext) : Generation {

    private val allModulesPagePlugin by lazy { context.plugin<AllModulesPagePlugin>() }
    private val templatingPlugin by lazy { context.plugin<TemplatingPlugin>() }

    override fun Timer.generate() {
        report("Processing submodules")
        val generationContext = processSubmodules()

        report("Creating all modules page")
        val pages = createAllModulesPage(generationContext)

        report("Transforming pages")
        val transformedPages = transformAllModulesPage(pages)

        report("Rendering")
        render(transformedPages)

        report("Processing multimodule")
        processMultiModule(transformedPages)

        report("Finish submodule processing")
        finishProcessingSubmodules()

        report("Running post-actions")
        runPostActions()
    }

    override val generationName: String = "index page for project"

    public fun createAllModulesPage(allModulesContext: DefaultAllModulesContext): RootPageNode =
        allModulesPagePlugin.querySingle { allModulesPageCreator }.invoke(allModulesContext)

    public fun transformAllModulesPage(pages: RootPageNode): RootPageNode =
        allModulesPagePlugin.query { allModulesPageTransformer }.fold(pages) { acc, t -> t(acc) }

    public fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }

    public fun runPostActions() {
        context[CoreExtensions.postActions].forEach { it() }
    }

    public fun processSubmodules(): DefaultAllModulesContext {
        return templatingPlugin.querySingle { submoduleTemplateProcessor }
            .process(context.configuration.modules)
            .let { DefaultAllModulesContext(it) }
    }

    public fun processMultiModule(root: RootPageNode) {
        templatingPlugin.querySingle { multimoduleTemplateProcessor }.process(root)
    }

    public fun finishProcessingSubmodules() {
        templatingPlugin.query { templateProcessingStrategy }.forEach { it.finish(context.configuration.outputDir) }
    }

    public data class DefaultAllModulesContext(val nonEmptyModules: List<String>) : CreationContext {
        public constructor(templating: TemplatingResult) : this(templating.modules)
    }
}
