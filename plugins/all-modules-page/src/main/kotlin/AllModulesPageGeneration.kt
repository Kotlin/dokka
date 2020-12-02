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
import org.jetbrains.dokka.versioning.VersioningPlugin

class AllModulesPageGeneration(private val context: DokkaContext) : Generation {

    private val allModulesPagePlugin by lazy { context.plugin<AllModulesPagePlugin>() }
    private val templatingPlugin by lazy { context.plugin<TemplatingPlugin>() }
    private val versioningPlugin by lazy { context.plugin<VersioningPlugin>() }

    override fun Timer.generate() {

        report("Creating all modules page")
        val pages = createAllModulesPage()

        report("Copy previous documentation")
        handlePreviousDocs()

        report("Transforming pages")
        val transformedPages = transformAllModulesPage(pages)

        report("Rendering")
        render(transformedPages)

        report("Processing submodules")
        processSubmodules()
    }

    override val generationName = "index page for project"

    fun handlePreviousDocs() = versioningPlugin.querySingle { versioningHandler }.invoke()

    fun createAllModulesPage() = allModulesPagePlugin.querySingle { allModulesPageCreator }.invoke()

    fun transformAllModulesPage(pages: RootPageNode) =
        allModulesPagePlugin.query { allModulesPageTransformer }.fold(pages) { acc, t -> t(acc) }

    fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }

    fun processSubmodules() =
        templatingPlugin.querySingle { templateProcessor }.process()
}