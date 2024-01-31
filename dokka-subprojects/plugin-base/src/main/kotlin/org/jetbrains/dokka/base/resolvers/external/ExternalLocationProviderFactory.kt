/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation

public fun interface ExternalLocationProviderFactory {
    public fun getExternalLocationProvider(doc: ExternalDocumentation): ExternalLocationProvider?
}
