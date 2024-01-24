/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import com.intellij.openapi.project.Project
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisContext
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

internal class ResolutionFacadeAnalysisContext(
    val facade: DokkaResolutionFacade,

    private val kotlinEnvironment: KotlinCoreEnvironment,
    private val analysisEnvironment: AnalysisEnvironment
) : AnalysisContext {
    private var isClosed: Boolean = false

    override val environment: KotlinCoreEnvironment
        get() = kotlinEnvironment.takeUnless { isClosed }
            ?: throw IllegalStateException("AnalysisEnvironment is already closed")

    override val resolveSession: ResolveSession = facade.resolveSession
    override val moduleDescriptor: ModuleDescriptor = facade.moduleDescriptor
    override val project: Project = facade.project

    override fun close() {
        isClosed = true
        analysisEnvironment.dispose()
    }
}
