/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.mock.MockProject
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisContext
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisEnvironment
import org.jetbrains.kotlin.analyzer.ResolverForModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

@InternalDokkaApi
public interface AnalysisContextCreator {
    public fun create(
        project: MockProject,
        moduleDescriptor: ModuleDescriptor,
        moduleResolver: ResolverForModule,
        kotlinEnvironment: KotlinCoreEnvironment,
        analysisEnvironment: AnalysisEnvironment,
    ): AnalysisContext
}
