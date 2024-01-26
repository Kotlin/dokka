/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import java.util.concurrent.ConcurrentHashMap

public class ExternalLocationProviderFactoryWithCache(
    public val ext: ExternalLocationProviderFactory
) : ExternalLocationProviderFactory {

    private val locationProviders = ConcurrentHashMap<ExternalDocumentation, CacheWrapper>()

    override fun getExternalLocationProvider(doc: ExternalDocumentation): ExternalLocationProvider? =
        locationProviders.getOrPut(doc) { CacheWrapper(ext.getExternalLocationProvider(doc)) }.provider

    private class CacheWrapper(val provider: ExternalLocationProvider?)
}

