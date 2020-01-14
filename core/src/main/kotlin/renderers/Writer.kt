package org.jetbrains.dokka.renderers

interface Writer {
    val root: String
    val extension: String
    fun write(path: String, text: String, ext: String = extension)
}