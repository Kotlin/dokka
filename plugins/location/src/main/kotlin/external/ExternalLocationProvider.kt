package org.jetbrains.dokka.location.external

import org.jetbrains.dokka.links.DRI

interface ExternalLocationProvider {
    fun resolve(dri: DRI): String?
}
