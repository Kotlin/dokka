package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single

interface OutputWriter {

    val context: DokkaContext
    val extension: String
        get() = context.single(CoreExtensions.fileExtension)
    
    fun write(path: String, text: String, ext: String = extension)
    fun writeResources(pathFrom: String, pathTo: String)
}