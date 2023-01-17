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

/**
 * First child delegation. It does not close [parent].
 */
abstract class KotlinAnalysis(
    val parent: KotlinAnalysis? = null
) : Closeable {

    operator fun get(key: DokkaSourceSet): AnalysisContext {
        return get(key.sourceSetID)
    }

    operator fun get(sourceSetID: DokkaSourceSetID): AnalysisContext {
        return find(sourceSetID)
            ?: parent?.get(sourceSetID)
            ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSet ${sourceSetID.sourceSetName}")
    }

    protected abstract fun find(sourceSetID: DokkaSourceSetID): AnalysisContext?
}

internal open class EnvironmentKotlinAnalysis(
    private val environments: SourceSetDependent<AnalysisContext>,
    parent: KotlinAnalysis? = null,
) : KotlinAnalysis(parent = parent) {

    override fun find(sourceSetID: DokkaSourceSetID): AnalysisContext? {
        return environments.entries.firstOrNull { (sourceSet, _) -> sourceSet.sourceSetID == sourceSetID }?.value
    }

    override fun close() {
        environments.values.forEach(AnalysisContext::close)
    }
}

internal class ProjectKotlinAnalysis(
    environments: SourceSetDependent<AnalysisContext>
) : EnvironmentKotlinAnalysis(environments)

internal class SamplesKotlinAnalysis(
    sampleEnvironments: SourceSetDependent<AnalysisContext>,
    projectKotlinAnalysis: KotlinAnalysis
) : EnvironmentKotlinAnalysis(sampleEnvironments, projectKotlinAnalysis)

