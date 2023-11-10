/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.java

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.SourceRootsExtractor
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import java.io.File

internal class KotlinAnalysisSourceRootsExtractor : SourceRootsExtractor {

    override fun extract(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): List<File> {
        val kotlinAnalysis = context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle { kotlinAnalysis }
        val environment = kotlinAnalysis[sourceSet].environment
        return environment.configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.mapNotNull { it.file.takeIf { isFileInSourceRoots(it, sourceSet) } }
            ?: listOf()
    }

    private fun isFileInSourceRoots(file: File, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean =
        sourceSet.sourceRoots.any { root -> file.startsWith(root) }

}
