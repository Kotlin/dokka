@file:Suppress("FunctionName")

package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaLogger
import java.io.Closeable

fun KotlinAnalysis(
    sourceSets: List<DokkaSourceSet>,
    logger: DokkaLogger,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
): KotlinAnalysis {
    val environments = sourceSets.associateWith { sourceSet ->
        createAnalysisContext(
            logger = logger,
            sourceSets = sourceSets,
            sourceSet = sourceSet,
            analysisConfiguration = analysisConfiguration
        )
    }
    return ProjectKotlinAnalysis(environments)
}

/**
 * [projectKotlinAnalysis] needs to be closed separately
 */
fun SamplesKotlinAnalysis(
    sourceSets: List<DokkaSourceSet>,
    logger: DokkaLogger,
    projectKotlinAnalysis: KotlinAnalysis,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
): KotlinAnalysis {
    val environments = sourceSets
        .filter { it.samples.isNotEmpty() }
        .associateWith { sourceSet ->
            createAnalysisContext(
                logger = logger,
                classpath = sourceSet.classpath,
                sourceRoots = sourceSet.samples,
                sourceSet = sourceSet,
                analysisConfiguration = analysisConfiguration
            )
    }

    return SamplesKotlinAnalysis(environments, projectKotlinAnalysis)
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

interface KotlinAnalysis : SourceSetDependent<AnalysisContext>, Closeable {
    override fun get(key: DokkaSourceSet): AnalysisContext
    operator fun get(sourceSetID: DokkaSourceSetID): AnalysisContext
}

internal class ProjectKotlinAnalysis(
    environments: SourceSetDependent<AnalysisContext>
) : KotlinAnalysisImpl(environments)

internal class SamplesKotlinAnalysis(
    sampleEnvironments: SourceSetDependent<AnalysisContext>,
    projectKotlinAnalysis: KotlinAnalysis
) : KotlinAnalysisImpl(sampleEnvironments, projectKotlinAnalysis)

/**
 * It does not close [defaultKotlinAnalysis].
 */
internal open class KotlinAnalysisImpl(
    private val environments: SourceSetDependent<AnalysisContext>,
    private val defaultKotlinAnalysis: KotlinAnalysis? = null
) : KotlinAnalysis, SourceSetDependent<AnalysisContext> by environments {

    override fun get(key: DokkaSourceSet): AnalysisContext {
        return environments[key] ?: defaultKotlinAnalysis?.get(key)
        ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSet $key")
    }

    override fun get(sourceSetID: DokkaSourceSetID): AnalysisContext {
        return environments.entries.firstOrNull { (sourceSet, _) -> sourceSet.sourceSetID == sourceSetID }?.value
            ?: defaultKotlinAnalysis?.get(sourceSetID)
            ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSetID $sourceSetID")
    }
    override fun close() {
        environments.values.forEach(AnalysisContext::close)
    }
}