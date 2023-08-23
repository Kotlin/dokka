/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch", "FunctionName", "UNUSED_PARAMETER", "unused", "DEPRECATION_ERROR",
    "DeprecatedCallableAddReplaceWith", "unused"
)

package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE
import org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaLogger
import java.io.Closeable

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public abstract class KotlinAnalysis(
    private val parent: KotlinAnalysis? = null
) : Closeable {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public operator fun get(key: DokkaConfiguration.DokkaSourceSet): AnalysisContext = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public operator fun get(key: DokkaSourceSetID): AnalysisContext = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    protected abstract fun find(sourceSetID: DokkaSourceSetID): AnalysisContext?
}

public class AnalysisContext(environment: Any, facade: Any, private val analysisEnvironment: Any) : Closeable {
    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val environment: Any get() = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val facade: Any get() = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public operator fun component1(): Any = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public operator fun component2(): Any = throw AnalysisApiDeprecatedError()

    @Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    override fun close() { throw AnalysisApiDeprecatedError() }
}

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public class DokkaAnalysisConfiguration(public val ignoreCommonBuiltIns: Boolean = false)

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public fun KotlinAnalysis(context: DokkaContext): KotlinAnalysis = throw AnalysisApiDeprecatedError()

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public fun KotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    logger: DokkaLogger,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
): KotlinAnalysis = throw AnalysisApiDeprecatedError()

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public fun ProjectKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    logger: DokkaLogger,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
): KotlinAnalysis = throw AnalysisApiDeprecatedError()

@Deprecated(message = ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
public fun SamplesKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    logger: DokkaLogger,
    projectKotlinAnalysis: KotlinAnalysis,
    analysisConfiguration: DokkaAnalysisConfiguration = DokkaAnalysisConfiguration()
): KotlinAnalysis = throw AnalysisApiDeprecatedError()

