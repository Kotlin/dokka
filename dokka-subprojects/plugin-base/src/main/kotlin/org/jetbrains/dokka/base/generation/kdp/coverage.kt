/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.kotlin.documentation.*

internal fun KdFragments.calculateCoverage() {
    val TOTAL = CoverageCounter("total")
    val PACKAGES = CoverageCounter("packages")
    val CLASSES = CoverageCounter("classes")
    val CONSTRUCTORS = CoverageCounter("constructors")
    val FUNCTIONS = CoverageCounter("functions")
    val PROPERTIES = CoverageCounter("properties")

    fun processDeclaration(element: KdElement) {
        if (element is KdModule) return

        TOTAL.count(element)

        when (element) {
            is KdConstructor -> CONSTRUCTORS.count(element)
            is KdFunction -> FUNCTIONS.count(element)
            is KdVariable -> PROPERTIES.count(element)
            is KdTypealias -> CLASSES.count(element)
            is KdClass -> CLASSES.count(element)
            is KdPackage -> PACKAGES.count(element)
        }
    }

    fragments.forEach {
        it.elements.forEach(::processDeclaration)
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
        if (total == 0) {
            println("$name: NO DOCUMENTATION")
        } else {
            println("$name: $hasDocumentation/$total = ${hasDocumentation * 100 / total}%")
        }

    }
}
