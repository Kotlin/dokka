package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike


interface ExternalDocumentablesProvider {
    fun findClasslike(dri: DRI, sourceSet: DokkaConfiguration.DokkaSourceSet): DClasslike?
}