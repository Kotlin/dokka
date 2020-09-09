package org.jetbrains.dokka.location.external.javadoc

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.location.shared.ExternalDocumentation
import org.jetbrains.dokka.plugability.DokkaContext

open class AndroidExternalLocationProvider(
    externalDocumentation: ExternalDocumentation,
    dokkaContext: DokkaContext
) : JavadocExternalLocationProvider(externalDocumentation, "", "", dokkaContext) {

    override fun anchorPart(callable: Callable) = callable.name.toLowerCase()

}
