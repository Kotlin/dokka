/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import java.io.Closeable

@OptIn(DokkaPluginApiPreview::class)
@Suppress("FunctionName")
internal fun ProjectKotlinAnalysis(
    sourceSets: List<DokkaSourceSet>,
    context: DokkaContext,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
): KotlinAnalysis {
    val environments = sourceSets.associateWith { sourceSet ->
        createAnalysisContext(
            context = context,
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
@OptIn(DokkaPluginApiPreview::class)
@Suppress("FunctionName")
internal fun SamplesKotlinAnalysis(
    sourceSets: List<DokkaSourceSet>,
    context: DokkaContext,
    projectKotlinAnalysis: KotlinAnalysis,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
): KotlinAnalysis {
    val environments = sourceSets
        .filter { it.samples.isNotEmpty() }
        .associateWith { sourceSet ->
            createAnalysisContext(
                context = context,
                classpath = sourceSet.classpath,
                sourceRoots = sourceSet.samples,
                sourceSet = sourceSet,
                analysisConfiguration = analysisConfiguration
            )
        }

    return EnvironmentKotlinAnalysis(environments, projectKotlinAnalysis)
}
@DokkaPluginApiPreview
public data class DokkaAnalysisConfiguration(
    /**
     * Only for common platform ignore BuiltIns for StdLib since it can cause a conflict
     * between BuiltIns from a compiler and ones from source code.
     */
    val ignoreCommonBuiltIns: Boolean = DEFAULT_IGNORE_COMMON_BUILT_INS
): ConfigurableBlock {
    public companion object {
        public const val DEFAULT_IGNORE_COMMON_BUILT_INS: Boolean = false
    }
}

/**
 * First child delegation. It does not close [parent].
 */
@InternalDokkaApi
public abstract class KotlinAnalysis(
    private val parent: KotlinAnalysis? = null
) : Closeable {

    public operator fun get(key: DokkaSourceSet): AnalysisContext {
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
