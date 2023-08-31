/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.RootPageNode

interface CreationContext

object NoCreationContext : CreationContext

interface PageCreator<T: CreationContext> {
    operator fun invoke(creationContext: T): RootPageNode
}
