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

    override fun Timer.generate() {
        report("Creating all modules page")
        val pages = createAllModulePage()

        report("Transforming pages")
        val transformedPages = transformAllModulesPage(pages)

        report("Rendering")
        render(transformedPages)

        report("Processing submodules")
        allModulesPagePlugin().querySingle { templateProcessor }.process()
    }

    override val generationName = "index page for project"

    fun createAllModulePage() = allModulesPagePlugin().querySingle { allModulePageCreator }.invoke()

    fun transformAllModulesPage(pages: RootPageNode) =
        allModulesPagePlugin().query { allModulePageTransformer }.fold(pages) { acc, t -> t(acc) }

    fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }

    private fun allModulesPagePlugin() = context.plugin<AllModulesPagePlugin>()
}