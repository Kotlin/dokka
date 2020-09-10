package org.jetbrains.dokka.rendering

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.rendering.output.FileWriter
import org.jetbrains.dokka.rendering.output.OutputWriter

class Rendering : DokkaPlugin() {
    val outputWriter by extensionPoint<OutputWriter>()

    val fileWriter by extending {
        outputWriter providing ::FileWriter
    }
}