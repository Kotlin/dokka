/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.plugability.DokkaContext

internal class AndroidXExternalLocationProvider(
    externalDocumentation: ExternalDocumentation,
    dokkaContext: DokkaContext
) : AndroidExternalLocationProvider(externalDocumentation, dokkaContext) {

    // AndroidX Kotlin reference uses case-sensitive `name()` anchors for properties,
    // unlike Android SDK documentation, which uses lowercase field anchors.
    override fun anchorPart(callable: Callable): String =
        if (callable.isProperty) "${callable.name}()" else super.anchorPart(callable)

}
