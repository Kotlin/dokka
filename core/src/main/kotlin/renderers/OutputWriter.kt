package org.jetbrains.dokka.renderers

interface OutputWriter{
    val extension: String
    fun write(path: String, text: String, ext: String = extension)
    fun writeResources(pathFrom: String, pathTo: String)
}