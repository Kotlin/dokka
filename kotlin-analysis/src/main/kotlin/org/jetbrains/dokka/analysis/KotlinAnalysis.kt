@file:Suppress("FunctionName")

package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaLogger

fun KotlinAnalysis(sourceSets: List<DokkaSourceSet>, logger: DokkaLogger, analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()): KotlinAnalysis {
    val environments = sourceSets.associateWith { sourceSet ->
        createEnvironmentAndFacade(
            logger = logger,
            sourceSets = sourceSets,
            sourceSet = sourceSet,
            analysisConfiguration = analysisConfiguration
        )
    }

    return KotlinAnalysisImpl(environments)
}

class DokkaAnalysisConfiguration(
    /**
     * Only for common platform ignore BuiltIns for StdLib since it can cause a conflict
     * between BuiltIns from a compiler and ones from source code.
     */
    val ignoreCommonBuiltIns: Boolean = false
)

@Deprecated(message = "Construct using list of DokkaSourceSets and logger",
    replaceWith = ReplaceWith("KotlinAnalysis(context.configuration.sourceSets, context.logger)")
)
fun KotlinAnalysis(context: DokkaContext): KotlinAnalysis = KotlinAnalysis(context.configuration.sourceSets, context.logger)

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
