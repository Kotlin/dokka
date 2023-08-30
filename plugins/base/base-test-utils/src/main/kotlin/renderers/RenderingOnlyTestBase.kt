/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers

import org.jetbrains.dokka.testApi.context.MockContext

abstract class RenderingOnlyTestBase<T> {
    abstract val context: MockContext
    abstract val renderedContent: T
}
