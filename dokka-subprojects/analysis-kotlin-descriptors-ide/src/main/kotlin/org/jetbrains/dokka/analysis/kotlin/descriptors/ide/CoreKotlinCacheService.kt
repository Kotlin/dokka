/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache


internal class CoreKotlinCacheService(private val resolutionFacade: DokkaResolutionFacade) : KotlinCacheService {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        return resolutionFacade
    }

    override fun getResolutionFacade(element: KtElement): ResolutionFacade {
        return resolutionFacade
    }

    override fun getResolutionFacadeByFile(
        file: PsiFile,
        platform: TargetPlatform
    ): ResolutionFacade {
        return resolutionFacade
    }

    override fun getResolutionFacadeByModuleInfo(
        moduleInfo: ModuleInfo,
        settings: PlatformAnalysisSettings
    ): ResolutionFacade {
        return resolutionFacade
    }

    override fun getResolutionFacadeByModuleInfo(
        moduleInfo: ModuleInfo,
        platform: TargetPlatform
    ): ResolutionFacade {
        return resolutionFacade
    }

    override fun getResolutionFacadeWithForcedPlatform(
        elements: List<KtElement>,
        platform: TargetPlatform
    ): ResolutionFacade {
        return resolutionFacade
    }

    override fun getSuppressionCache(): KotlinSuppressCache {
        throw UnsupportedOperationException()
    }

}
