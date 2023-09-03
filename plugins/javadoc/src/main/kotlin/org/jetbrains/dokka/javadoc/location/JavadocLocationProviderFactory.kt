/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

public class JavadocLocationProviderFactory(
    private val context: DokkaContext
) : LocationProviderFactory {
    override fun getLocationProvider(pageNode: RootPageNode): LocationProvider =
        JavadocLocationProvider(pageNode, context)
}
