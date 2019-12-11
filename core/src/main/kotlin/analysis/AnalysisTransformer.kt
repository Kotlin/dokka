package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.pages.PlatformData

/**
 * [AnalysisTransformer] allows to map over
 */
interface AnalysisTransformer {
    fun transform(platformToEnv: Map<PlatformData, EnvironmentAndFacade>): Map<PlatformData, EnvironmentAndFacade>
}