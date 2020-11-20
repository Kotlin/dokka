package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.Timer
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle

class AllModulesPageGeneration(private val context: DokkaContext) : Generation {

    private val allModulesPagePlugin by lazy { context.plugin<AllModulesPagePlugin>() }

    override fun Timer.generate() {
        report("Creating all modules page")
        val pages = createAllModulesPage()

        report("Transforming pages")
        val transformedPages = transformAllModulesPage(pages)

        report("Rendering")
        render(transformedPages)

        report("Processing submodules")
        processSubmodules()
    }

    override val generationName = "index page for project"

    fun createAllModulesPage() = allModulesPagePlugin.querySingle { allModulesPageCreator }.invoke()

    fun transformAllModulesPage(pages: RootPageNode) =
        allModulesPagePlugin.query { allModulesPageTransformer }.fold(pages) { acc, t -> t(acc) }

    fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }

    fun processSubmodules() =
        allModulesPagePlugin.querySingle { templateProcessor }.process()
}