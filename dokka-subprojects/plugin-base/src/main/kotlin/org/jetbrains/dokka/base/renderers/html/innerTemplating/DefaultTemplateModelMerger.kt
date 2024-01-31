/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html.innerTemplating

public class DefaultTemplateModelMerger : TemplateModelMerger {
    override fun invoke(
        factories: List<TemplateModelFactory>,
        buildModel: TemplateModelFactory.() -> TemplateMap
    ): TemplateMap {
        val mapper = mutableMapOf<String, Any?>()
        factories.map(buildModel).forEach { partialModel ->
            partialModel.forEach { (k, v) ->
                mapper[k] = v
            }
        }
        return mapper
    }
}
