/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.pages.PageNode

public fun interface PageMergerStrategy {

    public fun tryMerge(pages: List<PageNode>, path: List<String>): List<PageNode>

}
