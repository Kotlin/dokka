package org.jetbrains.dokka

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache


class CoreKotlinCacheService(private val resolutionFacade: DokkaResolutionFacade) : KotlinCacheService {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        return resolutionFacade
    }

    override fun getResolutionFacadeByFile(file: PsiFile, platform: TargetPlatform): ResolutionFacade {
        return resolutionFacade
    }

    override fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo, platform: TargetPlatform): ResolutionFacade? {
        return resolutionFacade
    }

    override fun getSuppressionCache(): KotlinSuppressCache {
        throw UnsupportedOperationException()
    }

}

