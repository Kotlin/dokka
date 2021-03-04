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
import org.jetbrains.dokka.versioning.VersioningPlugin

class AllModulesPageGeneration(private val context: DokkaContext) : Generation {

    private val allModulesPagePlugin by lazy { context.plugin<AllModulesPagePlugin>() }
    private val templatingPlugin by lazy { context.plugin<TemplatingPlugin>() }
    private val versioningPlugin by lazy { context.plugin<VersioningPlugin>() }

    override fun Timer.generate() {
        report("Processing submodules")
        val generationContext = processSubmodules()

        report("Creating all modules page")
        val pages = createAllModulesPage(generationContext)

        report("Copy previous documentation")
        handlePreviousDocs()

        report("Transforming pages")
        val transformedPages = transformAllModulesPage(pages)

        report("Rendering")
        render(transformedPages)

        report("Processing multimodule")
        processMultiModule(transformedPages)

        report("Finish submodule processing")
        finishProcessingSubmodules()
    }

    override val generationName = "index page for project"

    fun handlePreviousDocs() = versioningPlugin.querySingle { versioningHandler }.invoke()

    fun createAllModulesPage(allModulesContext: DefaultAllModulesContext) =
        allModulesPagePlugin.querySingle { allModulesPageCreator }.invoke(allModulesContext)

    fun transformAllModulesPage(pages: RootPageNode) =
        allModulesPagePlugin.query { allModulesPageTransformer }.fold(pages) { acc, t -> t(acc) }

    fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }

    fun processSubmodules() =
        templatingPlugin.querySingle { submoduleTemplateProcessor }
            .process(context.configuration.modules)
            .let { DefaultAllModulesContext(it) }

    fun processMultiModule(root: RootPageNode) =
        templatingPlugin.querySingle { multimoduleTemplateProcessor }.process(root)

    fun finishProcessingSubmodules() =
        templatingPlugin.query { templateProcessingStrategy }.forEach { it.finish(context.configuration.outputDir) }

    data class DefaultAllModulesContext(val nonEmptyModules: List<String>) : CreationContext {
        constructor(templating: TemplatingResult) : this(templating.modules)
    }
}
