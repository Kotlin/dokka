/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.kotlin.documentation.KdClass
import org.jetbrains.kotlin.documentation.KdConstructor
import org.jetbrains.kotlin.documentation.KdDeclaration
import org.jetbrains.kotlin.documentation.KdDocumented
import org.jetbrains.kotlin.documentation.KdFunction
import org.jetbrains.kotlin.documentation.KdModule
import org.jetbrains.kotlin.documentation.KdTypealias
import org.jetbrains.kotlin.documentation.KdVariable

internal fun KdModule.calculateCoverage() {
    val TOTAL = CoverageCounter("total")
    val PACKAGES = CoverageCounter("packages")
    val CLASSES = CoverageCounter("classes")
    val CONSTRUCTORS = CoverageCounter("constructors")
    val FUNCTIONS = CoverageCounter("functions")
    val PROPERTIES = CoverageCounter("properties")

    fun processDeclaration(declaration: KdDeclaration) {
        TOTAL.count(declaration)

        when (declaration) {
            is KdConstructor -> CONSTRUCTORS.count(declaration)
            is KdFunction -> FUNCTIONS.count(declaration)
            is KdVariable -> PROPERTIES.count(declaration)
            is KdTypealias -> CLASSES.count(declaration)
            is KdClass -> {
                CLASSES.count(declaration)
                declaration.declarations.forEach(::processDeclaration)
            }
        }
    }

    fragments.forEach { fragment ->
        fragment.packages.forEach { pkg ->
            PACKAGES.count(pkg)
            TOTAL.count(pkg)
            pkg.declarations.forEach(::processDeclaration)
        }
    }
    println("COVERAGE:")
    TOTAL.print()
    PACKAGES.print()
    CLASSES.print()
    CONSTRUCTORS.print()
    FUNCTIONS.print()
    PROPERTIES.print()
}

private class CoverageCounter(private val name: String) {
    private var total = 0
    private var hasDocumentation = 0

    fun count(documented: KdDocumented) {
        total += 1
        if (documented.documentation.isNotEmpty()) {
            hasDocumentation += 1
        }
    }

    fun print() {
        println("$name: $hasDocumentation/$total = ${hasDocumentation * 100 / total}%")
    }
}
