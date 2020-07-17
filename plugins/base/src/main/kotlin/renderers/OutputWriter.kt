package org.jetbrains.dokka.base.renderers

interface OutputWriter {

    suspend fun write(path: String, text: String, ext: String)
    suspend fun writeResources(pathFrom: String, pathTo: String)
}