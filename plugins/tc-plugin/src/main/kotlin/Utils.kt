package org.jetbrains.dokka.tc.plugin

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheServiceImpl
import org.jetbrains.kotlin.psi.KtFile

interface TCCompile {
  /**
   * Bails bc SdkTypes are not initialized.
   * Look for other options
   */
  fun analyzeWithAllCompilerChecks(target: KtFile, project: Project, extra: List<KtFile> = emptyList()): AnalysisResult =
    KotlinCacheServiceImpl(project).getResolutionFacade(extra.plus(target)).analyzeWithAllCompilerChecks(listOf(target))
}