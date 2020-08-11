package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.links.DRI

interface ExternalLocationProvider {
    fun resolve(dri: DRI): String?
}
