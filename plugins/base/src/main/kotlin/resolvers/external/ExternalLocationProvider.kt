package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.links.DRI

/**
 * Provides the path to the page documenting a [DRI] in an external documentation source
 */
interface ExternalLocationProvider {
    /**
     * @return Path to the page containing the [dri] or null if the path cannot be created
     * (eg. when the package-list does not contain [dri]'s package)
     */
    fun resolve(dri: DRI): String?
}
