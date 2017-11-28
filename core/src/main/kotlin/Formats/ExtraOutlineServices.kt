package org.jetbrains.dokka

import java.io.File

/**
 * Outline service that is responsible for generating a single outline format.
 *
 * TODO: port existing implementations of ExtraOutlineService to OutlineService, and remove this.
 */
interface ExtraOutlineService {
    fun getFileName(): String
    fun getFile(location: Location): File
    fun format(node: DocumentationNode): String
}

/**
 * Holder of all of the extra outline services needed for a StandardFormat, in addition to the main
 * [OutlineFormatService].
 */
abstract class ExtraOutlineServices(vararg val services: ExtraOutlineService)
