package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaLogger
import java.io.Closeable

@Suppress("FunctionName")
fun ProjectKotlinAnalysis(
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
    return EnvironmentKotlinAnalysis(environments)
}

/**
 * [projectKotlinAnalysis] needs to be closed separately
 *  Usually the analysis created for samples is short-lived and can be closed right after
 *  it's been used, there's no need to wait for [projectKotlinAnalysis] to be closed as it must be handled separately.
 */
@Suppress("FunctionName")
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

    return EnvironmentKotlinAnalysis(environments, projectKotlinAnalysis)
}

class DokkaAnalysisConfiguration(
    /**
     * Only for common platform ignore BuiltIns for StdLib since it can cause a conflict
     * between BuiltIns from a compiler and ones from source code.
     */
    val ignoreCommonBuiltIns: Boolean = false
)

@Deprecated(
    message = "Construct using list of DokkaSourceSets and logger",
    replaceWith = ReplaceWith("KotlinAnalysis(context.configuration.sourceSets, context.logger)")
)
fun KotlinAnalysis(context: DokkaContext): KotlinAnalysis =
    ProjectKotlinAnalysis(context.configuration.sourceSets, context.logger)

@Deprecated(
    message = "It was renamed to `ProjectKotlinAnalysis`",
    replaceWith = ReplaceWith("ProjectKotlinAnalysis(sourceSets, logger, analysisConfiguration)")
)
fun KotlinAnalysis(
    sourceSets: List<DokkaSourceSet>,
    logger: DokkaLogger,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
) = ProjectKotlinAnalysis(sourceSets, logger, analysisConfiguration)


/**
 * First child delegation. It does not close [parent].
 */
abstract class KotlinAnalysis(
    val parent: KotlinAnalysis? = null
) : Closeable {

    operator fun get(key: DokkaSourceSet): AnalysisContext {
        return get(key.sourceSetID)
    }

    operator fun get(key: DokkaSourceSetID): AnalysisContext {
        return find(key)
            ?: parent?.get(key)
            ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSet $key")
    }

    protected abstract fun find(sourceSetID: DokkaSourceSetID): AnalysisContext?
}

internal open class EnvironmentKotlinAnalysis(
    private val environments: SourceSetDependent<AnalysisContext>,
    parent: KotlinAnalysis? = null,
) : KotlinAnalysis(parent = parent) {

    override fun find(sourceSetID: DokkaSourceSetID): AnalysisContext? =
        environments.entries.firstOrNull { (sourceSet, _) -> sourceSet.sourceSetID == sourceSetID }?.value

    override fun close() {
        environments.values.forEach(AnalysisContext::close)
    }
}
