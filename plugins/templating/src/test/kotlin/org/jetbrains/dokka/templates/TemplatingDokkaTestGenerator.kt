package org.jetbrains.dokka.templates

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
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

class TemplatingDokkaTestGenerator(
    configuration: DokkaConfiguration,
    logger: DokkaLogger,
    testMethods: TemplatingTestMethods,
    additionalPlugins: List<DokkaPlugin> = emptyList()
) : DokkaTestGenerator<TemplatingTestMethods>(
    configuration,
    logger,
    testMethods,
    additionalPlugins + TemplatingPlugin() + TestTemplatingPlugin()
) {
    override fun generate() = with(testMethods) {
        val dokkaGenerator = DokkaGenerator(configuration, logger)

        val context =
            dokkaGenerator.initializePlugins(configuration, logger, additionalPlugins)

        pluginsSetupStage(context)

        val generation = context.single(CoreExtensions.generation) as TestTemplatingGeneration

        generation.processSubmodules()
        submoduleProcessingStage(context)

        generation.finishProcessing()
        finishProcessingSubmodules(context)
    }

}

open class TemplatingTestMethods(
    open val pluginsSetupStage: (DokkaContext) -> Unit,
    open val submoduleProcessingStage: (DokkaContext) -> Unit,
    open val finishProcessingSubmodules: (DokkaContext) -> Unit,
) : TestMethods

class TemplatingTestBuilder : TestBuilder<TemplatingTestMethods>() {
    var pluginsSetupStage: (DokkaContext) -> Unit = {}
    var submoduleProcessingStage: (DokkaContext) -> Unit = {}
    var finishProcessingSubmodules: (DokkaContext) -> Unit = {}

    override fun build() = TemplatingTestMethods(
        pluginsSetupStage,
        submoduleProcessingStage,
        finishProcessingSubmodules,
    )
}

abstract class TemplatingAbstractTest(logger: TestLogger = TestLogger(DokkaConsoleLogger(LoggingLevel.DEBUG))) :
    AbstractTest<TemplatingTestMethods, TemplatingTestBuilder, TemplatingDokkaTestGenerator>(
        ::TemplatingTestBuilder,
        ::TemplatingDokkaTestGenerator,
        logger,
    )
