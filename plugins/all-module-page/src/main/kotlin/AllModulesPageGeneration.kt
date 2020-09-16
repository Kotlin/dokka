package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.Timer
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class AllModulesPageGeneration(private val context: DokkaContext) : Generation {
    override fun Timer.generate() {
        report("Creating all modules page")
        val pages = createAllModulePage()

        report("Transforming pages")
        val transformedPages = transformAllModulesPage(pages)

        report("Rendering")
        render(transformedPages)
    }

    override val generationName = "index page for project"

    fun createAllModulePage() = context.single(CoreExtensions.allModulePageCreator).invoke()

    fun transformAllModulesPage(pages: RootPageNode) =
        context[CoreExtensions.allModulePageTransformer].fold(pages) { acc, t -> t(acc) }

    fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }
}