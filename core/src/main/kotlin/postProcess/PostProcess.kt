package org.jetbrains.dokka.postProcess

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * [PostProcess] allows DokkaPlugins to compute additional tasks with [run].
 * Each Process has a [name].
 */
interface PostProcess {
    val name: String
    suspend fun run(config: DokkaConfiguration, ctx: DokkaContext): Unit
}