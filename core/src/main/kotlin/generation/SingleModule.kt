package org.jetbrains.dokka.generation

import org.jetbrains.dokka.*
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.report

class SingleModule(private val context: DokkaContext): Generation {
    override fun Timer.generate() {
        report("Creating documentation models")
        val modulesFromPlatforms = createDocumentationModels(context)

        report("Transforming documentation model before merging")
        val transformedDocumentationBeforeMerge = transformDocumentationModelBeforeMerge(modulesFromPlatforms, context)

        report("Merging documentation models")
        val documentationModel = mergeDocumentationModels(transformedDocumentationBeforeMerge, context)

        report("Transforming documentation model after merging")
        val transformedDocumentation = transformDocumentationModelAfterMerge(documentationModel, context)

        report("Creating pages")
        val pages = createPages(transformedDocumentation, context)

        report("Transforming pages")
        val transformedPages = transformPages(pages, context)

        report("Rendering")
        render(transformedPages, context)

        reportAfterRendering(context)
    }

    override val generationName: String
        get() = TODO("Not yet implemented")

    fun createDocumentationModels(
        context: DokkaContext
    ) = context.configuration.sourceSets
        .flatMap { sourceSet -> translateSources(sourceSet, context) }
        .also { modules -> if (modules.isEmpty()) exitGenerationGracefully("Nothing to document") }

    fun transformDocumentationModelBeforeMerge(
        modulesFromPlatforms: List<DModule>,
        context: DokkaContext
    ) = context[CoreExtensions.preMergeDocumentableTransformer].fold(modulesFromPlatforms) { acc, t -> t(acc) }

    fun mergeDocumentationModels(
        modulesFromPlatforms: List<DModule>,
        context: DokkaContext
    ) = context.single(CoreExtensions.documentableMerger).invoke(modulesFromPlatforms)

    fun transformDocumentationModelAfterMerge(
        documentationModel: DModule,
        context: DokkaContext
    ) = context[CoreExtensions.documentableTransformer].fold(documentationModel) { acc, t -> t(acc, context) }

    fun createPages(
        transformedDocumentation: DModule,
        context: DokkaContext
    ) = context.single(CoreExtensions.documentableToPageTranslator).invoke(transformedDocumentation)

    fun createAllModulePage(
        context: DokkaContext
    ) = context.single(CoreExtensions.allModulePageCreator).invoke()

    fun transformPages(
        pages: RootPageNode,
        context: DokkaContext
    ) = context[CoreExtensions.pageTransformer].fold(pages) { acc, t -> t(acc) }

    fun transformAllModulesPage(
        pages: RootPageNode,
        context: DokkaContext
    ) = context[CoreExtensions.allModulePageTransformer].fold(pages) { acc, t -> t(acc) }

    fun render(
        transformedPages: RootPageNode,
        context: DokkaContext
    ) {
        val renderer = context.single(CoreExtensions.renderer)
        renderer.render(transformedPages)
    }

    fun reportAfterRendering(context: DokkaContext) {
        context.unusedPoints.takeIf { it.isNotEmpty() }?.also {
            context.logger.info("Unused extension points found: ${it.joinToString(", ")}")
        }

        context.logger.report()

        if (context.configuration.failOnWarning && (context.logger.warningsCount > 0 || context.logger.errorsCount > 0)) {
            throw DokkaException(
                "Failed with warningCount=${context.logger.warningsCount} and errorCount=${context.logger.errorsCount}"
            )
        }
    }

    private fun translateSources(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext) =
        context[CoreExtensions.sourceToDocumentableTranslator].map {
            it.invoke(sourceSet, context)
        }
}