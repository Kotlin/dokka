package org.jetbrains.dokka.analysis.kotlin.symbols.compiler

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.analysis.kotlin.internal.ModuleAndPackageDocumentationReader

internal fun ModuleAndPackageDocumentationReader(context: DokkaContext): ModuleAndPackageDocumentationReader =
    ContextModuleAndPackageDocumentationReader(context)

private class ContextModuleAndPackageDocumentationReader(
    private val context: DokkaContext
) : ModuleAndPackageDocumentationReader {
    override fun read(module: DModule): SourceSetDependent<DocumentationNode> {
        return emptyMap()
    }

    override fun read(pkg: DPackage): SourceSetDependent<DocumentationNode> {
        return emptyMap()
    }

    override fun read(module: DokkaConfiguration.DokkaModuleDescription): DocumentationNode? {
        return null
    }
}
