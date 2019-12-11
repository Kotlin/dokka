package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.pages.PlatformData

/**
 * The [DefaultAnalysisTransformer] solely returns the value of the parameter in [transform] unchanged
 */
object DefaultAnalysisTransformer : AnalysisTransformer {
    override fun transform(platformToEnv: Map<PlatformData, EnvironmentAndFacade>): Map<PlatformData, EnvironmentAndFacade> =
        platformToEnv
}