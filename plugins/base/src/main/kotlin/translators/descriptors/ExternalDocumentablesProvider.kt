package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike

/**
 * Service that can be queried with [DRI] and source set to obtain a documentable for classlike.
 *
 * There are some cases when there is a need to process documentables of classlikes that were not defined
 * in the project itself but are somehow related to the symbols defined in the documented project (e.g. are supertypes
 * of classes defined in project).
 */
fun interface ExternalDocumentablesProvider {

    /**
     * Returns [DClasslike] matching provided [DRI] in specified source set.
     *
     * Result is null if compiler haven't generated matching class descriptor.
     */
    fun findClasslike(dri: DRI, sourceSet: DokkaConfiguration.DokkaSourceSet): DClasslike?
}