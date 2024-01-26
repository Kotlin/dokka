/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.plugability.DokkaContext

public open class AndroidExternalLocationProvider(
    externalDocumentation: ExternalDocumentation,
    dokkaContext: DokkaContext
) : JavadocExternalLocationProvider(externalDocumentation, "", "", dokkaContext) {

    override fun anchorPart(callable: Callable): String = callable.name.toLowerCase()

}
