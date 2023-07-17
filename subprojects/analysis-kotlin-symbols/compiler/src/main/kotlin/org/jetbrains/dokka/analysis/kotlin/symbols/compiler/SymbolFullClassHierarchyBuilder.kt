package org.jetbrains.dokka.analysis.kotlin.symbols.compiler

import org.jetbrains.dokka.model.DModule
import org.jetbrains.kotlin.analysis.kotlin.internal.ClassHierarchy
import org.jetbrains.kotlin.analysis.kotlin.internal.FullClassHierarchyBuilder

class SymbolFullClassHierarchyBuilder : FullClassHierarchyBuilder {
    override suspend fun build(module: DModule): ClassHierarchy {
        return emptyMap()
    }

}
