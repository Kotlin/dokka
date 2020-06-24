@file:Suppress("FunctionName")

package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.SourceSetCache
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext

fun KotlinAnalysis(context: DokkaContext): KotlinAnalysis {
    val environments = context.configuration.passesConfigurations
        .associate { passConfiguration ->
            context.sourceSetCache.getSourceSet(passConfiguration) to createEnvironmentAndFacade(
                logger = context.logger,
                configuration = context.configuration,
                pass = passConfiguration
            )
        }

    return KotlinAnalysisImpl(context.sourceSetCache, environments)
}

interface KotlinAnalysis : SourceSetDependent<EnvironmentAndFacade> {
    override fun get(key: SourceSetData): EnvironmentAndFacade
    operator fun get(sourceSetID: String): EnvironmentAndFacade
    operator fun get(passConfiguration: DokkaConfiguration.PassConfiguration): EnvironmentAndFacade
}

internal class KotlinAnalysisImpl(
    private val sourceSetCache: SourceSetCache,
    private val environments: SourceSetDependent<EnvironmentAndFacade>
) : KotlinAnalysis, SourceSetDependent<EnvironmentAndFacade> by environments {

    override fun get(key: SourceSetData): EnvironmentAndFacade {
        return environments[key] ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSet $key")
    }

    override fun get(sourceSetID: String): EnvironmentAndFacade {
        return environments.entries.first { (sourceSet, _) -> sourceSet.sourceSetID == sourceSetID }.value
    }

    override fun get(passConfiguration: DokkaConfiguration.PassConfiguration): EnvironmentAndFacade {
        return get(sourceSetCache.getSourceSet(passConfiguration))
    }
}
