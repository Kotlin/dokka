package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

internal fun createEnvironmentAndFacade(
    logger: DokkaLogger,
    configuration: DokkaConfiguration,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): EnvironmentAndFacade =
    AnalysisEnvironment(DokkaMessageCollector(logger), sourceSet.analysisPlatform).run {
        if (analysisPlatform == Platform.jvm) {
            addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
        }
        sourceSet.classpath.forEach(::addClasspath)

        addSources(
            (sourceSet.sourceRoots + configuration.sourceSets.filter { it.sourceSetID in sourceSet.dependentSourceSets }
                .flatMap { it.sourceRoots })
        )

        loadLanguageVersionSettings(sourceSet.languageVersion, sourceSet.apiVersion)

        val environment = createCoreEnvironment()
        val (facade, _) = createResolutionFacade(environment)
        EnvironmentAndFacade(environment, facade)
    }

class DokkaMessageCollector(private val logger: DokkaLogger) : MessageCollector {
    override fun clear() {
        seenErrors = false
    }

    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.info(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}

// It is not data class due to ill-defined equals
class EnvironmentAndFacade(val environment: KotlinCoreEnvironment, val facade: DokkaResolutionFacade) {
    operator fun component1() = environment
    operator fun component2() = facade
}
