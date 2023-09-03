/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers

import org.jetbrains.dokka.testApi.context.MockContext

public abstract class RenderingOnlyTestBase<T> {
    public abstract val context: MockContext
    public abstract val renderedContent: T
}
