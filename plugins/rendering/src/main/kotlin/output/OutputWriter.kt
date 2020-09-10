package org.jetbrains.dokka.rendering.output

interface OutputWriter {

    suspend fun write(path: String, text: String, ext: String)
    suspend fun writeResources(pathFrom: String, pathTo: String)
}