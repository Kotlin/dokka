/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html.innerTemplating

public fun interface TemplateModelMerger {
    public fun invoke(factories: List<TemplateModelFactory>, buildModel: TemplateModelFactory.() -> TemplateMap): TemplateMap
}
