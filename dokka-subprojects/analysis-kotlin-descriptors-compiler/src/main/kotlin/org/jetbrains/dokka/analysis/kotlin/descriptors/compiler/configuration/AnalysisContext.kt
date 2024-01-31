/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration

import com.intellij.openapi.project.Project
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.AnalysisContextCreator
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.io.Closeable
import java.io.File

@OptIn(DokkaPluginApiPreview::class)
internal fun createAnalysisContext(
    context: DokkaContext,
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    analysisConfiguration: DokkaAnalysisConfiguration
): AnalysisContext {
    val parentSourceSets = sourceSets.filter { it.sourceSetID in sourceSet.dependentSourceSets }
    val classpath = sourceSet.classpath + parentSourceSets.flatMap { it.classpath }
    val sources = sourceSet.sourceRoots + parentSourceSets.flatMap { it.sourceRoots }

    return createAnalysisContext(
        context = context,
        classpath = classpath,
        sourceRoots = sources,
        sourceSet = sourceSet,
        analysisConfiguration = analysisConfiguration
    )
}

@OptIn(DokkaPluginApiPreview::class)
internal fun createAnalysisContext(
    context: DokkaContext,
    classpath: List<File>,
    sourceRoots: Set<File>,
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    analysisConfiguration: DokkaAnalysisConfiguration
): AnalysisContext {
    val analysisEnvironment = AnalysisEnvironment(
        DokkaMessageCollector(context.logger),
        sourceSet.analysisPlatform,
        context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle { mockApplicationHack },
        context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle { klibService },
    ).apply {
        if (analysisPlatform == Platform.jvm) {
            configureJdkClasspathRoots()
        }
        addClasspath(classpath)
        addSources(sourceRoots)

        loadLanguageVersionSettings(sourceSet.languageVersion, sourceSet.apiVersion)
    }

    val environment = analysisEnvironment.createCoreEnvironment()
    return analysisEnvironment.createResolutionFacade(
        environment,
        context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle<CompilerDescriptorAnalysisPlugin, AnalysisContextCreator> { analysisContextCreator },
        analysisConfiguration.ignoreCommonBuiltIns
    )
}

internal class DokkaMessageCollector(private val logger: DokkaLogger) : MessageCollector {
    override fun clear() {
        seenErrors = false
    }

    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.info(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}

@InternalDokkaApi
public interface AnalysisContext : Closeable {
    public val environment: KotlinCoreEnvironment
    public val resolveSession: ResolveSession
    public val moduleDescriptor: ModuleDescriptor
    public val project: Project
}
