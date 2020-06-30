@file:Suppress("FunctionName")

package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext

fun KotlinAnalysis(context: DokkaContext): KotlinAnalysis {
    val environments = context.configuration.sourceSets
        .associate { sourceSet ->
            sourceSet to createEnvironmentAndFacade(
                logger = context.logger,
                configuration = context.configuration,
                sourceSet = sourceSet
            )
        }

    return KotlinAnalysisImpl(environments)
}

interface KotlinAnalysis : SourceSetDependent<EnvironmentAndFacade> {
    override fun get(key: DokkaSourceSet): EnvironmentAndFacade
    operator fun get(sourceSetID: DokkaSourceSetID): EnvironmentAndFacade
}

internal class KotlinAnalysisImpl(
    private val environments: SourceSetDependent<EnvironmentAndFacade>
) : KotlinAnalysis, SourceSetDependent<EnvironmentAndFacade> by environments {

    override fun get(key: DokkaSourceSet): EnvironmentAndFacade {
        return environments[key] ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSet $key")
    }

    override fun get(sourceSetID: DokkaSourceSetID): EnvironmentAndFacade {
        return environments.entries.first { (sourceSet, _) -> sourceSet.sourceSetID == sourceSetID }.value
    }
}
