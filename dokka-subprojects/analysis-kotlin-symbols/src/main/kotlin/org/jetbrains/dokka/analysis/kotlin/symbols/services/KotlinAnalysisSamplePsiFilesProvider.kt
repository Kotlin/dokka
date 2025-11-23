/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiFile
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.java.SamplePsiFilesProvider
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

internal class KotlinAnalysisSamplePsiFilesProvider : SamplePsiFilesProvider {
    override fun getSamplePsiFiles(sourceSet: DokkaSourceSet, context: DokkaContext): Set<PsiFile> =
        with(context.plugin<SymbolsAnalysisPlugin>().querySingle { sampleKotlinAnalysis }) {
            modulesWithFiles[getModule(sourceSet)]?.toSet() ?: emptySet()
        }
}
