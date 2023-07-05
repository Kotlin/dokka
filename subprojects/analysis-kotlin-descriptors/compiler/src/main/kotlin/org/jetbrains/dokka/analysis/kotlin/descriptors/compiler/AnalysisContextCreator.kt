package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.mock.MockProject
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisContext
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.AnalysisEnvironment
import org.jetbrains.kotlin.analyzer.ResolverForModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

@InternalDokkaApi
interface AnalysisContextCreator {
    fun create(
        project: MockProject,
        moduleDescriptor: ModuleDescriptor,
        moduleResolver: ResolverForModule,
        kotlinEnvironment: KotlinCoreEnvironment,
        analysisEnvironment: AnalysisEnvironment,
    ): AnalysisContext
}
