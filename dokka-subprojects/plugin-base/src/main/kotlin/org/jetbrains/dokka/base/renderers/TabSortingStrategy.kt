/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.pages.ContentNode

public interface TabSortingStrategy {
    public fun <T: ContentNode> sort(tabs: Collection<T>) : List<T>
}
