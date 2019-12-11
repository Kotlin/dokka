package org.jetbrains.dokka

import org.jetbrains.dokka.analysis.AnalysisEnvironment
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.renderers.FileWriter
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class DokkaGenerator(
    private val configuration: DokkaConfiguration,
    private val logger: DokkaLogger
) {

    fun generate() {
        logger.debug("Setting up analysis environments")
        val platforms: Map<PlatformData, EnvironmentAndFacade> = configuration.passesConfigurations.map {
            PlatformData(it.analysisPlatform, it.targets) to createEnvironmentAndFacade(it)
        }.toMap()

        logger.debug("Initializing plugins")
        val context = DokkaContext.create(configuration.pluginsClasspath, logger, platforms)

        logger.debug("transforming EnvironmentAndFacade and refreshing context")
        val transformedPlatforms: Map<PlatformData, EnvironmentAndFacade> =
            context.single(CoreExtensions.analysis).transform(platforms)
        val ctx = DokkaContext.create(configuration.pluginsClasspath, logger, transformedPlatforms)

        logger.debug("Creating documentation models")
        val modulesFromPlatforms = transformedPlatforms.map { (pdata, _) -> translateDescriptors(pdata, ctx) }

        logger.debug("Merging documentation models")
        val documentationModel = ctx.single(CoreExtensions.documentationMerger)
            .invoke(modulesFromPlatforms, ctx)

        logger.debug("Transforming documentation model")
        val transformedDocumentation = ctx[CoreExtensions.documentationTransformer]
            .fold(documentationModel) { acc, t -> t(acc, ctx) }

        logger.debug("Creating pages")
        val pages = ctx.single(CoreExtensions.documentationToPageTranslator)
            .invoke(transformedDocumentation, ctx)

        logger.debug("Transforming pages")
        val transformedPages = ctx[CoreExtensions.pageTransformer]
            .fold(pages) { acc, t -> t(acc, ctx) }

        logger.debug("Rendering")
        val fileWriter = FileWriter(configuration.outputDir, "")
        val locationProvider = ctx.single(CoreExtensions.locationProviderFactory)
            .invoke(transformedPages, configuration, ctx)
        val renderer = ctx.single(CoreExtensions.rendererFactory)
            .invoke(fileWriter, locationProvider, ctx)

        renderer.render(transformedPages)
    }

    private fun createEnvironmentAndFacade(pass: DokkaConfiguration.PassConfiguration): EnvironmentAndFacade =
        AnalysisEnvironment(DokkaMessageCollector(logger), pass.analysisPlatform).run {
            if (analysisPlatform == Platform.jvm) {
                addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
            }
            pass.classpath.forEach { addClasspath(File(it)) }

            addSources(pass.sourceRoots.map { it.path })

            loadLanguageVersionSettings(pass.languageVersion, pass.apiVersion)

            val environment = createCoreEnvironment()
            val (facade, _) = createResolutionFacade(environment)
            EnvironmentAndFacade(environment, facade)
        }

    private fun translateDescriptors(platformData: PlatformData, context: DokkaContext): Module {
        val (environment, facade) = context.platforms.getValue(platformData)

        val packageFragments = environment.getSourceFiles().asSequence()
            .map { it.packageFqName }
            .distinct()
            .mapNotNull { facade.resolveSession.getPackageFragment(it) }
            .toList()

        return context.single(CoreExtensions.descriptorToDocumentationTranslator)
            .invoke(packageFragments, platformData, context)
    }

    private class DokkaMessageCollector(private val logger: DokkaLogger) : MessageCollector {
        override fun clear() {
            seenErrors = false
        }

        private var seenErrors = false

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            if (severity == CompilerMessageSeverity.ERROR) {
                seenErrors = true
            }
            logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
        }

        override fun hasErrors() = seenErrors
    }
}

// It is not data class due to ill-defined equals
class EnvironmentAndFacade(val environment: KotlinCoreEnvironment, val facade: DokkaResolutionFacade) {
    operator fun component1() = environment
    operator fun component2() = facade
}