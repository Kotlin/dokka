package org.jetbrains.dokka.analysis.kotlin.symbols

import com.intellij.openapi.project.Project
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.kotlin.symbols.compiler.createAnalysisSession
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import java.io.Closeable


internal fun ProjectKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    context: DokkaContext,
): KotlinAnalysis {
    val environments = sourceSets.associateWith { sourceSet ->
        createAnalysisContext(
            context = context,
            sourceSets = sourceSets,
            sourceSet = sourceSet
        )
    }
    return EnvironmentKotlinAnalysis(environments)
}

@Suppress("UNUSED_PARAMETER")
internal fun createAnalysisContext(
    context: DokkaContext,
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): AnalysisContext {
    val parentSourceSets = sourceSets.filter { it.sourceSetID in sourceSet.dependentSourceSets }
    val classpath = sourceSet.classpath + parentSourceSets.flatMap { it.classpath }
    val sources = sourceSet.sourceRoots + parentSourceSets.flatMap { it.sourceRoots }

    return AnalysisContextImpl(createAnalysisSession(classpath, sources))
}


/**
 * First child delegation. It does not close [parent].
 */
@InternalDokkaApi
abstract class KotlinAnalysis(
    private val parent: KotlinAnalysis? = null
) : Closeable {

    operator fun get(key: DokkaConfiguration.DokkaSourceSet): AnalysisContext {
        return get(key.sourceSetID)
    }

    internal operator fun get(key: DokkaSourceSetID): AnalysisContext {
        return find(key)
            ?: parent?.get(key)
            ?: throw IllegalStateException("Missing EnvironmentAndFacade for sourceSet $key")
    }

    internal abstract fun find(sourceSetID: DokkaSourceSetID): AnalysisContext?
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

interface AnalysisContext: Closeable {
    val project: Project
}

class AnalysisContextImpl(private val analysisSession: StandaloneAnalysisAPISession) : AnalysisContext {
    override val project: Project
        get() = analysisSession.project

    override fun close() {
        analysisSession.application.dispose()
        analysisSession.project.dispose()
    }
}