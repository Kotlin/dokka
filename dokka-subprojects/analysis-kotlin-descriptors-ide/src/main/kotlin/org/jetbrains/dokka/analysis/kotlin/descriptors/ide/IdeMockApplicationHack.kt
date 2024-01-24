/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
