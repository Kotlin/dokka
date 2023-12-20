/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode

public fun interface DocumentableToPageTranslator {
    public operator fun invoke(module: DModule): RootPageNode
}

