/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.RootPageNode

public interface CreationContext

public object NoCreationContext : CreationContext

public interface PageCreator<T: CreationContext> {
    public operator fun invoke(creationContext: T): RootPageNode
}
