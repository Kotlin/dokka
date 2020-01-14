package org.jetbrains.dokka.postProcess

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext

object DefaultPostProcess : PostProcess {
    override val name: String = "DefaultPostProcess"
    override suspend fun run(config: DokkaConfiguration, ctx: DokkaContext): Unit = Unit
}