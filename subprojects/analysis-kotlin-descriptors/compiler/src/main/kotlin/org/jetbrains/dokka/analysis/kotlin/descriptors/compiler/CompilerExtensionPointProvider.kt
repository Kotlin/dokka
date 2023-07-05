package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.kotlin.extensions.ApplicationExtensionDescriptor

@InternalDokkaApi
interface CompilerExtensionPointProvider {
    fun get(): List<CompilerExtensionPoint>

    class CompilerExtensionPoint(
        val extensionDescriptor: ApplicationExtensionDescriptor<Any>,
        val extensions: List<Any>
    )
}
