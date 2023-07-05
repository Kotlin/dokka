package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import com.intellij.mock.MockApplication
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.MockApplicationHack
import org.jetbrains.kotlin.idea.klib.KlibLoadingMetadataCache

internal class IdeMockApplicationHack : MockApplicationHack {
    override fun hack(mockApplication: MockApplication) {
        if (mockApplication.getService(KlibLoadingMetadataCache::class.java) == null)
            mockApplication.registerService(KlibLoadingMetadataCache::class.java, KlibLoadingMetadataCache())
    }
}
