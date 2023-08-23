/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.pages.RootPageNode

public fun interface LocationProviderFactory {
    public fun getLocationProvider(pageNode: RootPageNode): LocationProvider
}
