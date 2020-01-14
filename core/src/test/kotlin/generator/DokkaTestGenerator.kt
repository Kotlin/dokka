package org.jetbrains.dokka

import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.renderers.FileWriter
import org.jetbrains.dokka.utilities.DokkaLogger

class DokkaTestGenerator(
    private val configuration: DokkaConfiguration,
    private val logger: DokkaLogger,
    private val setupTest: (Map<PlatformData, EnvironmentAndFacade>) -> Unit?,
    private val pluginInitTest: (DokkaContext) -> Unit?,
    private val documentablesCreationTest: (List<Module>) -> Unit?,
    private val documentablesMergingTest: (Module) -> Unit?,
    private val documentablesTransformationTest: (Module) -> Unit?,
    private val pagesCreationTest: (ModulePageNode) -> Unit?,
    private val pagesTransformationTest: (ModulePageNode) -> Unit?,
    private val finalTest: (DokkaConfiguration) -> Unit?
) : DokkaGenerator(configuration, logger) {

    override fun generate() {
        logger.debug("Setting up analysis environments")
        val platforms = configuration.passesConfigurations.map {
            PlatformData(it.moduleName, it.analysisPlatform, it.targets) to createEnvironmentAndFacade(it)
        }.toMap()

        setupTest(platforms)

        logger.debug("Initializing plugins")
        val context = DokkaContext.create(configuration.pluginsClasspath, logger, platforms)

        pluginInitTest(context)

        logger.debug("Creating documentation models")
        val modulesFromPlatforms = platforms.map { (pdata, _) -> translateDescriptors(pdata, context) }

        documentablesCreationTest(modulesFromPlatforms)

        logger.debug("Merging documentation models")
        val documentationModel = context.single(CoreExtensions.documentationMerger)
            .invoke(modulesFromPlatforms, context)

        documentablesMergingTest(documentationModel)

        logger.debug("Transforming documentation model")
        val transformedDocumentation = context[CoreExtensions.documentationTransformer]
            .fold(documentationModel) { acc, t -> t(acc, context) }

        documentablesTransformationTest(transformedDocumentation)

        logger.debug("Creating pages")
        val pages = context.single(CoreExtensions.documentationToPageTranslator)
            .invoke(transformedDocumentation, context)

        pagesCreationTest(pages)

        logger.debug("Transforming pages")
        val transformedPages = context[CoreExtensions.pageTransformer]
            .fold(pages) { acc, t -> t(acc, context) }

        pagesTransformationTest(transformedPages)

        logger.debug("Rendering")
        val fileWriter = FileWriter(configuration.outputDir, "")
        val locationProvider = context.single(CoreExtensions.locationProviderFactory)
            .invoke(transformedPages, configuration, context)
        val renderer = context.single(CoreExtensions.rendererFactory)
            .invoke(fileWriter, locationProvider, context)

        renderer.render(transformedPages)

        finalTest(configuration)
    }

}