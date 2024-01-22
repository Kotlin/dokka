/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html.innerTemplating

import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.pages.PageNode

public interface TemplateModelFactory {
    public fun buildModel(
        page: PageNode,
        resources: List<String>,
        locationProvider: LocationProvider,
        content: String
    ): TemplateMap

    public fun buildSharedModel(): TemplateMap
}
