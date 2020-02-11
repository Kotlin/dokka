package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single

interface OutputWriter {

    fun write(path: String, text: String, ext: String)
    fun writeResources(pathFrom: String, pathTo: String)
}