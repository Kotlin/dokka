package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerExtensionPointProvider


internal class IdeCompilerExtensionPointProvider : CompilerExtensionPointProvider {
    override fun get(): List<CompilerExtensionPointProvider.CompilerExtensionPoint> = emptyList()
}
