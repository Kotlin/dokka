/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.RootPageNode

public fun interface Renderer {
    public fun render(root: RootPageNode)
}
