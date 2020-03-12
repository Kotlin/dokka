package org.jetbrains.dokka.testApi.testRunner

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.pages.PlatformData
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

        val platforms: Map<PlatformData, EnvironmentAndFacade> = dokkaGenerator.setUpAnalysis(configuration)
        analysisSetupStage(platforms)

        val context = dokkaGenerator.initializePlugins(configuration, logger, platforms, pluginOverrides)
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
    }
}