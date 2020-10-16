package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.DCI
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.urlEncoded

abstract class DokkaBaseLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext,
    extension: String
) : DefaultLocationProvider(pageGraphRoot, dokkaContext, extension) {

    /**
     * Anchors should be unique and should contain sourcesets, dri and contentKind.
     * The idea is to make them as short as possible and just use a hashCode from sourcesets in order to match the
     * 2040 characters limit
     */
    open fun anchorForDCI(dci: DCI, sourceSets: Set<DisplaySourceSet>): String =
        (dci.dri.toString() + "/" + dci.kind + "/" + sourceSets.hashCode()).urlEncoded()

}
