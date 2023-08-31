/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.AnalysisContextCreator
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisContext
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisEnvironment
import org.jetbrains.kotlin.analyzer.ResolverForModule
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

internal class IdeAnalysisContextCreator : AnalysisContextCreator {
    override fun create(
        project: MockProject,
        moduleDescriptor: ModuleDescriptor,
        moduleResolver: ResolverForModule,
        kotlinEnvironment: KotlinCoreEnvironment,
        analysisEnvironment: AnalysisEnvironment,
    ): AnalysisContext {
        val facade = DokkaResolutionFacade(project, moduleDescriptor, moduleResolver)
        val projectComponentManager = project as MockComponentManager
        projectComponentManager.registerService(
            KotlinCacheService::class.java,
            CoreKotlinCacheService(facade)
        )
        return ResolutionFacadeAnalysisContext(facade, kotlinEnvironment, analysisEnvironment)
    }
}
