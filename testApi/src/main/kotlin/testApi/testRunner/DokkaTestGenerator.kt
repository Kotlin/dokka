package org.jetbrains.dokka.testApi.testRunner

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.model.SourceSetCache
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.utilities.DokkaLogger

internal class DokkaTestGenerator(
    private val configuration: DokkaConfiguration,
    private val logger: DokkaLogger,
    private val testMethods: TestMethods,
    private val pluginOverrides: List<DokkaPlugin> = emptyList()
) {

    fun generate() = with(testMethods) {
        val dokkaGenerator = DokkaGenerator(configuration, logger)
        val sourceSetsCache = SourceSetCache()

        val platforms: Map<SourceSetData, EnvironmentAndFacade> = dokkaGenerator.setUpAnalysis(
            configuration,
            sourceSetsCache
        )
        analysisSetupStage(platforms)

        val context =
            dokkaGenerator.initializePlugins(configuration, logger, platforms, sourceSetsCache, pluginOverrides)
        pluginsSetupStage(context)

        val modulesFromPlatforms = dokkaGenerator.createDocumentationModels(platforms, context)
        documentablesCreationStage(modulesFromPlatforms)

        val filteredModules = dokkaGenerator.transformDocumentationModelBeforeMerge(modulesFromPlatforms, context)
        documentablesFirstTransformationStep(filteredModules)

        val documentationModel = dokkaGenerator.mergeDocumentationModels(filteredModules, context)
        documentablesMergingStage(documentationModel)

        val transformedDocumentation = dokkaGenerator.transformDocumentationModelAfterMerge(documentationModel, context)
        documentablesTransformationStage(transformedDocumentation)

        val pages = dokkaGenerator.createPages(transformedDocumentation, context)
        pagesGenerationStage(pages)

        val transformedPages = dokkaGenerator.transformPages(pages, context)
        pagesTransformationStage(transformedPages)

        dokkaGenerator.render(transformedPages, context)
        renderingStage(transformedPages, context)

        dokkaGenerator.reportAfterRendering(context)
    }
}