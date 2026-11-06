/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation.pages
import org.jetbrains.kotlin.documentation.*

class PageSetBuilder {

    fun buildPageSet(fragment: KdFragment): PageSet =
        PageSet(
            fragment.elements.entries
                .mapNotNull { (id, element) -> (element as? KdModule)?.let { id to it } }
                .ifEmpty { error("no modules in ${fragment.name}") }
                .mapNotNull { (id, module) ->
                    buildPage(fragment, id, module, parentSegments = emptyList())
                }
        )

}

fun KdElement.urlSegment(parentSegments: List<String>): String = when (this) {
    is KdModule -> name
    is KdPackage -> name
    is KdClass -> name
    is KdTypealias -> name
    is KdFunction -> name
    is KdProperty -> name
    is KdConstructor -> parentSegments.last()
    is KdRefinedElement -> error("unreachable: skipped before naming")
    is KdInheritedMember -> error("unreachable: skipped before naming")
}

private fun buildPage(
    fragment: KdFragment,
    id: KdElementId,
    element: KdElement,
    parentSegments: List<String>,
): Page? =
    when (val decision = element.pageDecision()) {
        is PageDecision.Skip -> {
            println("  skip $id: ${decision.reason}")
            null
        }
        is PageDecision.Create -> {
            val segments = parentSegments + element.urlSegment(parentSegments)
            Page(
                kind = decision.pageKind,
                segments = segments,
                represents = listOf(ElementRef(fragment = fragment.name, id = id)),
                children = element.childIds().mapNotNull { childId ->
                    val child = fragment.elements[childId]
                        ?: error("no element for $childId (child of $id) in ${fragment.name}")
                    buildPage(fragment, childId, child, segments)
                },
            )
        }
    }

/** Ось контейнеров — единственное, по чему спускаемся. Никогда по fragment.elements.values. */
private fun KdElement.childIds(): List<KdElementId> = when (this) {
    is KdModule -> packages
    is KdPackage -> declarations
    is KdClass -> declarations
    is KdTypealias -> emptyList()
    is KdFunction -> emptyList()
    is KdProperty -> emptyList()
    is KdConstructor -> emptyList()
    is KdRefinedElement -> emptyList()
    is KdInheritedMember -> emptyList()
}