/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation

import org.jetbrains.dokka.model.DModule
import org.jetbrains.kotlin.documentation.KdModule

internal fun DModule.toKdModule(): KdModule {
    return KdModule(
        name = name,
        fragments = emptyList(),
        documentation = null
    )
}
