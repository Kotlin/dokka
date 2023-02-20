package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.testApi.testRunner.AbstractTest
import org.jetbrains.dokka.testApi.testRunner.DokkaTestGenerator
import org.jetbrains.dokka.testApi.testRunner.TestBuilder
import org.jetbrains.dokka.testApi.testRunner.TestMethods
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel

class MultiModuleDokkaTestGenerator(
    configuration: DokkaConfiguration,
    logger: DokkaLogger,
    testMethods: MultiModuleTestMethods,
    additionalPlugins: List<DokkaPlugin> = emptyList()
) : DokkaTestGenerator<MultiModuleTestMethods>(
    configuration,
    logger,
    testMethods,
    additionalPlugins + AllModulesPagePlugin()
) {
    override fun generate() = with(testMethods) {
        val dokkaGenerator = DokkaGenerator(configuration, logger)

        val context =
            dokkaGenerator.initializePlugins(configuration, logger, additionalPlugins + AllModulesPagePlugin())
        pluginsSetupStage(context)

        val generation = context.single(CoreExtensions.generation) as AllModulesPageGeneration

        val generationContext = generation.processSubmodules()
        submoduleProcessingStage(context)

        val allModulesPage = generation.createAllModulesPage(generationContext)
        allModulesPageCreationStage(allModulesPage)

        val transformedPages = generation.transformAllModulesPage(allModulesPage)
        pagesTransformationStage(transformedPages)

        generation.render(transformedPages)
        renderingStage(transformedPages, context)

        generation.processMultiModule(transformedPages)
        processMultiModule(transformedPages)

        generation.finishProcessingSubmodules()
        finishProcessingSubmodules(context)
    }

}

open class MultiModuleTestMethods(
    open val pluginsSetupStage: (DokkaContext) -> Unit,
    open val allModulesPageCreationStage: (RootPageNode) -> Unit,
    open val pagesTransformationStage: (RootPageNode) -> Unit,
    open val renderingStage: (RootPageNode, DokkaContext) -> Unit,
    open val submoduleProcessingStage: (DokkaContext) -> Unit,
    open val processMultiModule: (RootPageNode) -> Unit,
    open val finishProcessingSubmodules: (DokkaContext) -> Unit,
) : TestMethods

class MultiModuleTestBuilder : TestBuilder<MultiModuleTestMethods>() {
    var pluginsSetupStage: (DokkaContext) -> Unit = {}
    var allModulesPageCreationStage: (RootPageNode) -> Unit = {}
    var pagesTransformationStage: (RootPageNode) -> Unit = {}
    var renderingStage: (RootPageNode, DokkaContext) -> Unit = { _, _ -> }
    var submoduleProcessingStage: (DokkaContext) -> Unit = {}
    var processMultiModule: (RootPageNode) -> Unit = {}
    var finishProcessingSubmodules: (DokkaContext) -> Unit = {}

    override fun build() = MultiModuleTestMethods(
        pluginsSetupStage,
        allModulesPageCreationStage,
        pagesTransformationStage,
        renderingStage,
        submoduleProcessingStage,
        processMultiModule,
        finishProcessingSubmodules
    )
}

abstract class MultiModuleAbstractTest(logger: TestLogger = TestLogger(DokkaConsoleLogger(LoggingLevel.DEBUG))) :
    AbstractTest<MultiModuleTestMethods, MultiModuleTestBuilder, MultiModuleDokkaTestGenerator>(
        ::MultiModuleTestBuilder,
        ::MultiModuleDokkaTestGenerator,
        logger,
    )
